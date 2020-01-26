(ns games.tetris.core
  (:require
   [games.tetris.db :as tetris.db]
   [games.grid.core :as grid]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Predicates and game logic
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cell-occupied? [{:keys [game-grid]} cell]
  (:occupied (grid/get-cell game-grid cell)))

(defn cell-open?
  "Returns true if the indicated cell is within the grid's bounds AND not
  occupied."
  [{:keys [game-grid] :as db} cell]
  (and
   (grid/within-bounds? game-grid cell)
   (not (cell-occupied? db cell))))

(defn next-cells
  "Supports can-add-next?
  In danger of differing from `add-new-piece`...
  "
  [{:keys [entry-cell piece-queue]}]
  ((first piece-queue) entry-cell))

(defn can-add-next?
  "Returns true if any of the cells for the next piece
  are already occupied."
  [{:keys [entry-cell] :as db}]
  (->> (next-cells db)
     (filter (fn [c] (cell-occupied? db c)))
     (count)
     (= 0)))

(defn row-fully-occupied? [row f?]
   (grid/true-for-row? row :occupied))

(defn rows-to-clear
  "Returns true if there are rows to be removed from the board."
  [{:keys [game-grid]}]
  (grid/select-rows game-grid row-fully-occupied?))

(defn clear-full-rows
  "Removes rows satisfying the predicate, replacing them with rows of empty
  cells."
  [db]
  (update db :game-grid
    #(grid/remove-rows % row-fully-occupied?)))

(defn any-falling?
  "Returns true if there is a falling cell anywhere in the grid."
  [{:keys [game-grid]}]
  (seq (grid/get-cells game-grid :falling)))

(defn gameover?
  "Returns true if no new pieces can be added.
  "
  [db]
  (not (can-add-next? db)))

(defn mark-cell-occupied
  "Marks the passed cell (x, y) as occupied, dissoc-ing the :falling key.
  Returns an updated db."
  [db cell]
  (update db :game-grid
    #(grid/update-cell % cell
      (fn [c] (-> c
                (assoc :occupied true)
                (dissoc :falling))))))

(defn get-falling-cells
  "Returns all cells with a `:falling true` prop"
  [{:keys [game-grid]}]
  (grid/get-cells game-grid :falling))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Move piece
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn move-piece
  "Moves a floating cell in the direction passed.
  If pieces try to move down but are blocked, they are locked in place (with an
  :occupied flag).
  "
  [{:keys [game-grid] :as db} direction]
  (let [falling-cells (get-falling-cells db)
        move-f #(grid/move-cell-coords % direction)

        updated-grid
        (grid/move-cells game-grid
            {:move-f move-f
             :can-move? #(cell-open? db %)
             :cells falling-cells})

        db (assoc db :game-grid updated-grid)

        should-lock-cells?
        (and
         ;; down-only
         (= direction :down)
         ;; any falling cells?
         falling-cells
         ;; any falling cells that can't move down?
         ;; i.e. with occupied cells below them
         (seq (remove #(cell-open? db (move-f %)) falling-cells)))]

    (if should-lock-cells?
      ;; mark all cells
      (as-> db db
        (reduce (fn [d cell] (mark-cell-occupied d cell))
                db falling-cells)
        ;; this also indicates that the pieces has been played, so we increment
        (update db :pieces-played inc))

      ;; otherwise just return the db
      db)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Rotating pieces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn rotate-diff
  "x1 = y0
  y1 = -x0"
  [{:keys [x y] :as cell}]
  {:x y
   :y (* -1 x)})

(defn calc-diff [anchor-cell cell]
  {:x (- (:x anchor-cell) (:x cell))
   :y (- (:y anchor-cell) (:y cell))})

(defn apply-diff [anchor-cell cell]
  {:x (+ (:x anchor-cell) (:x cell))
   :y (+ (:y anchor-cell) (:y cell))})

(defn calc-rotate-target [anchor-cell cell]
  (apply-diff anchor-cell (rotate-diff (calc-diff anchor-cell cell))))

(defn rotate-piece
  "Rotates a falling piece in-place.
  This requires one falling cell to be an 'anchor'.
  The anchor attempts to stay in place - the other cells calc an x/y delta
  between themselves and the anchor, which is used to update the cells locations
  as they rotate around the anchor.

  If the rotation cannot be done due to conflict with boundaries or occupied
  pieces, fallback moves are attempted, moving the pieces one two spaces to the
  left or right before attempting the rotate. This results in the 'bumping' away
  from walls when attempting to rotate on the edge of the grid."
  [{:keys [game-grid] :as db}]
  (let [falling-cells (get-falling-cells db)
        anchor-cell (first (filter :anchor falling-cells))]

    (if-not anchor-cell
      ;; no anchor-cell, do nothing
      db
      (update db :game-grid
              (fn [grid]
                (grid/move-cells
                 grid
                 {:move-f #(calc-rotate-target anchor-cell %)
                  :fallback-moves
                  [{:additional-cells [anchor-cell]
                    :fallback-move-f (fn [c]
                                       (as-> c c
                                         (grid/move-cell-coords c :right)
                                         (calc-rotate-target
                                          (update anchor-cell :x inc) c)))}
                   {:additional-cells [anchor-cell]
                    :fallback-move-f (fn [c]
                                       (as-> c c
                                         (grid/move-cell-coords c :left)
                                         (calc-rotate-target
                                          (update anchor-cell :x dec) c)))}
                   {:additional-cells [anchor-cell]
                    :fallback-move-f (fn [c]
                                       (as-> c c
                                         (grid/move-cell-coords c :right)
                                         (grid/move-cell-coords c :right)
                                         (calc-rotate-target
                                          (update anchor-cell :x #(+ % 2)) c)))}
                   {:additional-cells [anchor-cell]
                    :fallback-move-f (fn [c]
                                       (as-> c c
                                         (grid/move-cell-coords c :left)
                                         (grid/move-cell-coords c :left)
                                         (calc-rotate-target
                                          (update anchor-cell :x #(- % 2)) c)))}]
                  :can-move? #(cell-open? db %)
                  :cells (remove :anchor falling-cells)}))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Adding new pieces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn next-piece-fn
  "Selects a random new piece."
  [{:keys [allowed-shape-fns]}]
  (rand-nth allowed-shape-fns))

(defn add-preview-piece [grid piece]
   (-> grid
     (grid/build-grid)
     (grid/add-cells
       {:entry-cell {:x 2 :y 3}
        :update-cell #(assoc % :preview true)
        :make-cells piece})))

(defn add-new-piece
  "Adds a new cell to the grid.
  Does not care if there is room to add it!
  Depends on the `new-piece-coord`."
  [{:keys [entry-cell game-grid piece-queue] :as db}]
  (let [for-queue (next-piece-fn db)
        next-three (take 3 (drop 1 piece-queue))
        make-cells (first piece-queue)]
    (-> db
      (update :piece-queue
                #(as-> % q
                     (drop 1 q)
                     (concat q [for-queue])))

      (assoc :falling-shape-fn make-cells)

      (update :game-grid
             (fn [g]
               (grid/add-cells g
                              {:entry-cell entry-cell
                               :update-cell #(assoc % :falling true)
                               :make-cells make-cells})))

      (update :preview-grids
             (fn [gs]
               (let [[g1 g2 g3] gs
                     [p1 p2 p3] next-three]
                 [(add-preview-piece g1 p1)
                  (add-preview-piece g2 p2)
                  (add-preview-piece g3 p3)]))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Removing Pieces (for hold/swap feature)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clear-falling-piece
  "Removes all cells that have a :falling prop."
  [db]
  (update db :game-grid #(grid/clear-cells % :falling)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Score
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn update-score
  "Score is a function of the number of rows cleared and the level.
  Combos function by double-counting previously cleared rows.
  Ex: if rows are cleared by piece n, and another row is cleared by piece n + 1,
  the original rows are included in the row-count-score multipled by the current
  level.
  "
  [{:keys [score-per-row-clear level rows-in-combo
           last-combo-piece-num pieces-played] :as db}]
  (let [rows-cleared (count (rows-to-clear db))
        carry-combo? (= pieces-played (+ last-combo-piece-num 1))
        row-count-score (if carry-combo?
                          (+ rows-cleared rows-in-combo)
                          rows-cleared)
        updated-rows-in-combo (if carry-combo?
                                row-count-score
                                rows-cleared)]
    (-> db
      (update :score #(+ % (* score-per-row-clear row-count-score level)))
      (assoc :rows-in-combo updated-rows-in-combo)
      (assoc :last-combo-piece-num pieces-played))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game tick/steps functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn step [db]
  (cond
    ;; clear pieces, update db and return
    (> (count (rows-to-clear db)) 0)
    (-> db
      (update-score)
      (clear-full-rows))

    ;; game is over, update db and return
    ;;(gameover? db) (assoc db :gameover true) ;; or :phase :gameover?
    (gameover? db) tetris.db/initial-db

    ;; a piece is falling, move it down
    (any-falling? db)
    (move-piece db :down)

    ;; nothing is falling, add a new piece
    (not (any-falling? db))
    (add-new-piece db)

    ;; do nothing
    true db))

