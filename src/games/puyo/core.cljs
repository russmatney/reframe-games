(ns games.puyo.core
  (:require
   [clojure.set :as set]
   [games.grid.core :as grid]
   [games.puyo.db :as puyo.db]
   [games.puyo.shapes :as puyo.shapes]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game logic, predicates, helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn can-player-move?
  "Returns true if the game should accept player movement input."
  [{:keys [paused? fall-lock?]}]
  (and
    (not fall-lock?)
    (not paused?)))

(defn cell-occupied?
  [{:keys [game-grid]} cell]
  (:occupied (grid/get-cell game-grid cell)))

(defn cell-falling?
  [{:keys [game-grid]} cell]
  (:falling (grid/get-cell game-grid cell)))

(defn cell-within-bounds?
  [{:keys [game-grid] :as db} cell]
  (grid/within-bounds? game-grid cell))

(defn cell-open?
  "Returns true if the indicated cell is within the grid's bounds AND not
  occupied."
  [{:keys [game-grid] :as db} cell]
  (and
    (grid/within-bounds? game-grid cell)
    (not (cell-occupied? db cell))))

(defn can-overwrite-cell?
  "Returns true if the PASSED cell is within the grid's bounds, not
  occupied, and not falling. this does NOT read from the board, but expects
  the cell to be freshly looked up and passed to it."
  [{:keys [game-grid]}
   {:keys [occupied falling] :as cell}]
  (and
    (grid/within-bounds? game-grid cell)
    (not occupied)
    (not falling)))

(defn any-falling?
  "Returns true if there is a falling cell anywhere in the grid."
  [{:keys [game-grid]}]
  (seq (grid/get-cells game-grid :falling)))

(defn get-falling-cells
  "Returns all cells with a `:falling true` prop"
  [{:keys [game-grid]}]
  (grid/get-cells game-grid :falling))

(defn do-mark-cell-occupied
  [c]
  (-> c
      (assoc :occupied true)
      (dissoc :falling)))

(defn mark-cell-occupied
  "Marks the passed cell (x, y) as occupied, dissoc-ing the :falling key.
  Returns an updated db."
  [db cell]
  (update db :game-grid #(grid/update-cell % cell do-mark-cell-occupied)))

(defn mark-cells-occupied
  [db cells]
  (println "marking cells occupied" cells)
  (reduce (fn [db cell] (mark-cell-occupied db cell)) db cells))

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

(defn instant-fall
  "Gathers `:falling` cells and moves them with `grid/instant-fall`"
  [db direction]
  (let [falling-cells (get-falling-cells db)
        updated-db    (update db
                              :game-grid
                              (fn [g]
                                (grid/instant-fall
                                  g
                                  {:direction   direction
                                   :cells       falling-cells
                                   :keep-shape? false
                                   ;; TODO fix this to not need the db (should be handled underneath, this
                                   ;; can          be a cheap cell prop check)
                                   ;; works for now b/c it only checks bounds and :occupied
                                   ;; TODO investigate/simplify cell-open? usage and :falling flag
                                   :can-move?   #(can-overwrite-cell? db %)})))]
    ;; mark new cell coords as occupied
    (mark-cells-occupied updated-db
                         (get-falling-cells updated-db))))

(defn ->blocked-cells
  "Returns only the cells that cannot make the passed `move-f`, because
  `cell-open?` returns false for the target (new) cell."
  [db {:keys [cells move-f]}]
  (remove #(cell-open? db (move-f %)) cells))

(defn handle-blocked-cells
  "Updates cells after a move. Should only be called if the moved direction was
  `:down`. It is also assumed that there are blocked-cells passed.

  `Blocked` cells are marked occupied, other falling cells are instant-dropped."
  [db blocked-cells]
  (-> (reduce (fn [d cell]
                (mark-cell-occupied d cell))
              db blocked-cells)
      (instant-fall :down)))

(defn- do-move-piece
  [db move-opts]
  (update db :game-grid #(grid/move-cells % move-opts)))

(defn move-piece
  "Moves 'falling' cells in the passed direction.
  Updates blocked cells and instant-falls after first block via
  `handle-blocked-cells`."
  [{:keys [game-grid game-opts] :as db} direction]
  (let [falling-cells (get-falling-cells db)
        move-f        #(grid/move-cell-coords
                         % direction
                         (merge game-opts {:grid game-grid}))
        move-opts     {:move-f    move-f
                       :can-move? #(cell-open? db %)
                       :cells     falling-cells}
        blocked-cells (seq (->blocked-cells db move-opts))
        db            (do-move-piece db move-opts)]
    (println "falling cells" falling-cells)
    (if (and
          (= :down direction)
          blocked-cells
          (seq falling-cells))
      (handle-blocked-cells db blocked-cells)
      db)))

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

(defn add-preview-piece
  "Rebuilds a passed preview grid and adds the passed piece (func) to it."
  [grid piece-fn]
  (-> grid
      (grid/build-grid)
      (grid/add-cells
        {:update-cell #(assoc % :preview true)
         :make-cells  piece-fn})))

(defn add-new-piece
  "Adds a new cell to the grid.
  Does not care if there is room to add it!
  Depends on the `new-piece-coord`.

  Note that this does not always indicate a 'new' piece, as the swap mechanic
  prepends the piece-queue to add held pieces back.
  "
  [{:keys [piece-queue min-queue-size] :as db}]
  (let [next-three (take 3 (drop 1 piece-queue))
        make-cells (first piece-queue)
        game-opts  (:game-opts db)]
    (-> ;; this also indicates that the pieces has been played, so we increment
      db
      (update :current-piece-num inc)

      (update :piece-queue
              (fn [q]
                (let [q (drop 1 q)]
                  (if (< (count q) min-queue-size)
                    (concat q (puyo.shapes/next-bag db))
                    q))))

      ;; update the current falling fn
      (assoc :falling-shape-fn make-cells)

      ;; add the cells to the matrix!
      (update :game-grid
              (fn [g]
                (grid/add-cells g
                                {:update-cell #(assoc % :falling true)
                                 :make-cells  (make-cells game-opts)})))

      (update :preview-grids
              (fn [gs]
                (let [[g1 g2 g3] gs
                      [p1 p2 p3] next-three]
                  [(add-preview-piece g1 (p1 game-opts))
                   (add-preview-piece g2 (p2 game-opts))
                   (add-preview-piece g3 (p3 game-opts))]))))))

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
      (assoc :hold-lock true)))

(defn update-fallers
  "Updates cells that have had a cell below removed."
  [db groups]
  (let [deepest-by-x (grid/->deepest-by-x (reduce set/union groups))
        should-update?
        (fn [{:keys [color x y] :as cell}]
          (let [deep-y  (:y (get deepest-by-x x))
                should? (and
                          color
                          (not (nil? deep-y))
                          (< y deep-y))]
            should?))]
    (println "deepest-by-x:" deepest-by-x)
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
        :restart (puyo.db/game-db game-opts)
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
          (update-fallers groups)
          (instant-fall :down))

      ;; nothing is falling, add a new piece
      (not (any-falling? db))
      (add-new-piece db)

      ;; do nothing
      :else db)))

