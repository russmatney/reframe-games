(ns games.puyo.core
  (:require
   [games.puyo.db :as puyo.db]
   [games.grid.core :as grid]
   [clojure.set :as set]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game logic, predicates, helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn can-player-move?
  "Returns true if the game should accept player movement input."
  [{:keys [paused? fall-lock]}]
  (and
    (not paused?)
    (not fall-lock)))

(defn cell-occupied?
  [{:keys [game-grid]} cell]
  (:occupied (grid/get-cell game-grid cell)))

(defn cell-open?
  "Returns true if the indicated cell is within the grid's bounds AND not
  occupied."
  [{:keys [game-grid] :as db} cell]
  (and
    (grid/within-bounds? game-grid cell)
    (not (cell-occupied? db cell))))

(defn any-falling?
  "Returns true if there is a falling cell anywhere in the grid."
  [{:keys [game-grid]}]
  (seq (grid/get-cells game-grid :falling)))

(defn get-falling-cells
  "Returns all cells with a `:falling true` prop"
  [{:keys [game-grid]}]
  (grid/get-cells game-grid :falling))

(defn mark-cell-occupied
  "Marks the passed cell (x, y) as occupied, dissoc-ing the :falling key.
  Returns an updated db."
  [db cell]
  (update db :game-grid
          #(grid/update-cell % cell
                             (fn [c] (-> c
                                         (assoc :occupied true)
                                         (dissoc :falling))))))

(defn gameover?
  "Returns true if any cell of the grid has a y < 0.
  TODO fix this! only true if entry cell blocked"
  [{:keys [game-grid]}]
  (grid/any-cell? game-grid (fn [{:keys [y occupied]}]
                              (and occupied
                                   (< y 0)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Piece movement
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn move-piece
  [{:keys [game-grid game-opts] :as db} direction]
  (let [falling-cells (get-falling-cells db)
        move-f        #(grid/move-cell-coords
                         % direction
                         (merge game-opts {:grid game-grid}))

        updated-grid
        (grid/move-cells game-grid
                         {:move-f    move-f
                          :can-move? #(cell-open? db %)
                          :cells     falling-cells})

        db (assoc db :game-grid updated-grid)

        blocked-cells (seq (remove #(cell-open? db (move-f %)) falling-cells))

        all-cells-blocked?
        (and
          ;; down-only
          (= direction :down)
          ;; any falling cells?
          falling-cells
          ;; any falling cells that can't move down?
          ;; i.e. with occupied cells below them
          (= (count falling-cells) (count blocked-cells)))

        any-cells-blocked?
        (and
          ;; down-only
          (= direction :down)
          ;; any falling cells?
          falling-cells
          ;; any falling cells that can't move down?
          ;; i.e. with occupied cells below them
          blocked-cells)]

    (cond-> db
      any-cells-blocked?
      (as-> db
          ;; mark blocked cells occupied
          (reduce (fn [d cell]
                    (if (not (cell-open? d (move-f cell)))
                      (mark-cell-occupied d cell)
                      d))
                  db falling-cells)

        ;; prevent holds while pieces remain
        (assoc db :hold-lock true)
        ;; flag that there may be more to fall
        (assoc db :fall-lock true))

      all-cells-blocked?
      (->
        ;; remove the hold-lock to allow another hold to happen
        (assoc :hold-lock false)
        ;; flag that all have fallen
        (assoc :fall-lock false)))))

;; TODO think about swapping colors vs 'rotating', especially in narrow
;; situations
(defn rotate-piece
  [db]
  (let [falling-cells (get-falling-cells db)
        anchor-cell   (first (filter :anchor? falling-cells))]

    (if-not anchor-cell
      ;; no anchor-cell, do nothing
      db
      (update db :game-grid
              (fn [grid]
                (grid/move-cells
                  grid
                  {:move-f    #(grid/calc-rotate-target anchor-cell %)
                   :fallback-moves
                   [{:additional-cells [anchor-cell]
                     :fallback-move-f  (fn [c]
                                         (as-> c c
                                           (grid/move-cell-coords c :right)
                                           (grid/calc-rotate-target
                                             (update anchor-cell :x inc) c)))}
                    {:additional-cells [anchor-cell]
                     :fallback-move-f  (fn [c]
                                         (as-> c c
                                           (grid/move-cell-coords c :left)
                                           (grid/calc-rotate-target
                                             (update anchor-cell :x dec) c)))}]
                   :can-move? #(cell-open? db %)
                   :cells     (remove :anchor? falling-cells)}))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Adding Pieces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn next-bag
  "'bag' terminology carried over from tetris."
  [_]
  (repeatedly 5 puyo.db/build-piece-fn))

(defn add-preview-piece
  "Rebuilds a passed preview grid and adds the passed piece (func) to it."
  [grid piece]
  (-> grid
      (grid/build-grid)
      (grid/add-cells
        {:update-cell #(assoc % :preview true)
         :make-cells  piece})))

(defn add-new-piece
  "Adds a new cell to the grid.
  Does not care if there is room to add it!
  Depends on the `new-piece-coord`.

  Note that this does not always indicate a 'new' piece, as the swap mechanic
  prepends the piece-queue to add held pieces back.
  "
  [{:keys [piece-queue min-queue-size] :as db}]
  (let [next-three (take 3 (drop 1 piece-queue))
        make-cells (first piece-queue)]
    (-> ;; this also indicates that the pieces has been played, so we increment
      db
      (update :current-piece-num inc)

      (update :piece-queue
              (fn [q]
                (let [q (drop 1 q)]
                  (if (< (count q) min-queue-size)
                    (concat q (next-bag db))
                    q))))

      ;; update the current falling fn
      (assoc :falling-shape-fn make-cells)

      ;; should never prevent movement on a new piece
      (assoc :fall-lock false)

      ;; add the cells to the matrix!
      (update :game-grid
              (fn [g]
                (grid/add-cells g
                                {:update-cell #(assoc % :falling true)
                                 :make-cells  make-cells})))

      (update :preview-grids
              (fn [gs]
                (let [[g1 g2 g3] gs
                      [p1 p2 p3] next-three]
                  [(add-preview-piece g1 p1)
                   (add-preview-piece g2 p2)
                   (add-preview-piece g3 p3)]))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Clearing pieces and cells
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clear-falling-cells
  "Supports the 'hold/swap' mechanic."
  [db]
  (update db :game-grid #(grid/clear-cells % :falling)))

(defn groups-to-clear
  "Returns true if there are any groups of 4 or more adjacent same-color cells."
  [{:keys [game-grid group-size]}]
  (let [puyos        (grid/get-cells game-grid :occupied)
        color-groups (vals (group-by :color puyos))
        groups       (mapcat grid/group-adjacent-cells color-groups)]
    (filter (fn [group] (<= group-size (count group))) groups)))

(defn clear-groups
  "Clears groups that are have reached the group-size."
  [db groups]
  (-> db
      (update :game-grid
              (fn [grid]
                (grid/update-cells
                  grid
                  (fn [cell]
                    (seq (filter #(contains? % cell) groups)))
                  #(dissoc % :occupied :color :anchor?))))

      ;; prevent other fallers from being held
      (assoc :hold-lock true)

      ;; prevent other fallers from being user-movable
      (assoc :fall-lock true)))

(defn update-fallers
  "Updates cells that have had a cell below removed."
  [db groups]
  (let [deepest-by-x (grid/->deepest-by-x (reduce set/union groups))
        should-update?
        (fn [{:keys [color x y]}]
          (let [deep-y (:y (get deepest-by-x x))]
            (and
              color
              (not (nil? deep-y))
              (< y deep-y))))]
    (update db :game-grid
            (fn [grid]
              (grid/update-cells
                grid
                should-update?
                #(-> %
                     (dissoc :occupied)
                     (assoc :falling true)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Score and Level
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO should depend on a piece-played event
(defn update-score
  "Score is a function of the number of groups cleared and the level.
  Combos function by double-counting previously cleared groups.
  Ex: if groups are cleared by piece n, and another group is cleared by piece n + 1,
  the original groups are included in the group-count-score multipled by the current
  level.

  ;; TODO update to take size of groups into account
  "
  [{:keys [score-per-group-clear
           level
           groups-in-combo
           last-combo-piece-num ;; TODO rename last-score-piece-num?
           current-piece-num]
    :as   db}]
  (let [groups-cleared  (count (groups-to-clear db))
        carry-combo?    (= current-piece-num last-combo-piece-num)
        groups-in-combo (if carry-combo?
                          (+ groups-cleared groups-in-combo)
                          groups-cleared)
        addl-score      (* score-per-group-clear groups-in-combo level)]
    (-> db
        (update :score #(+ % addl-score))
        (assoc :groups-in-combo groups-in-combo)
        (assoc :last-combo-piece-num current-piece-num))))

(defn should-advance-level?
  [{:keys [level groups-per-level groups-cleared]}]
  (>= groups-cleared (* level groups-per-level)))

(defn advance-level
  "Each level updates the step timeout to 90% of the current speed."
  [db]
  (-> db
      (update :level inc)
      (update :step-timeout #(.floor js/Math (* % 0.9)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Step
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn step
  [db game-opts]
  (let [groups (groups-to-clear db)]
    (cond

      ;; game is over, update db and return
      ;; TODO gameover event, score event?
      (gameover? db)
      (case (-> game-opts :on-gameover)
        :restart (puyo.db/initial-db game-opts)
        nil      (assoc db :gameover? true))

      (should-advance-level? db)
      (advance-level db)

      ;; a piece is falling, move it down
      (any-falling? db)
      (move-piece db :down)

      ;; clear puyo groups, update db and return
      (seq groups)
      (-> db
          (update-score)
          (clear-groups groups)
          (update-fallers groups))

      ;; nothing is falling, add a new piece
      (not (any-falling? db))
      (add-new-piece db)

      ;; do nothing
      :else db)))

