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

(defn rows-to-clear?
  "Returns true if there are rows to be removed from the board."
  [{:keys [game-grid]}]
  (grid/any-row? game-grid row-fully-occupied?))

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
      (reduce (fn [db cell] (mark-cell-occupied db cell))
              db falling-cells)

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
  The anchor stays in place - the other cells calc an x/y delta between
  themselves and the anchor.

  TODO implement 'bumping' - rotations against walls/blocks should move the
  piece over to create space, if possible.
  "
  [{:keys [game-grid] :as db}]
  (let [falling-cells (get-falling-cells db)
        anchor-cell (first (filter :anchor falling-cells))]

    (if-not anchor-cell
      ;; no anchor-cell, do nothing
      db
      (update db :game-grid
              (fn [grid]
                (grid/move-cells grid
                 {:move-f #(calc-rotate-target anchor-cell %)
                  :can-move? #(cell-open? db %)
                  :cells (remove :anchor falling-cells)}))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Adding new pieces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn next-piece-fn
  "Selects a random new piece."
  [{:keys [allowed-shape-fns]}]
  (rand-nth allowed-shape-fns))

(defn add-new-piece
  "Adds a new cell to the grid.
  Does not care if there is room to add it!
  Depends on the `new-piece-coord`."
  [{:keys [entry-cell game-grid piece-queue] :as db}]
  (let [for-queue (next-piece-fn db)
        make-cells (first piece-queue)]
    (-> db
        (update :piece-queue
                #(as-> % q
                     (drop 1 q)
                     (concat q [for-queue])))

     (update :game-grid
             (fn [g]
               (grid/add-cells g
                              {:entry-cell entry-cell
                               :update-cell #(assoc % :falling true)
                               :make-cells make-cells})))
     (update :preview-grid
             (fn [g]
               (-> g
                   (grid/build-grid)
                   (grid/add-cells
                     {:entry-cell {:x 2 :y 3}
                      :update-cell #(assoc % :preview true)
                      :make-cells for-queue})))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game tick/steps functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn step [db]
  (cond
    ;; clear pieces, update db and return
    (rows-to-clear? db) ;; animation?
    (clear-full-rows db)

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

