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

(defn can-add-new? [{:keys [entry-cell] :as db}]
  "Returns true if the entry cell is not occupied,
  or if the entry cell itself can move down.
  TODO refactor to depend on the next piece being added
  (which may cover more than just the entry-cell)
  "
  (not (cell-occupied? db entry-cell)))

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

  Needs to know what the next piece is, doesn't it?
  TODO generate list of pieces to be added, pull from the db here
  "
  [db]
  (not (can-add-new? db)))

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

(defn select-new-piece
  "Selects a random new piece."
  [{:keys [allowed-shape-fns]}]
  (rand-nth allowed-shape-fns))

(defn add-new-piece
  "Adds a new cell to the grid.
  Does not care if there is room to add it!
  Depends on the `new-piece-coord`."
  [{:keys [entry-cell game-grid] :as db}]
  (let [make-cells (select-new-piece db)]
    (update db :game-grid
            (fn [grid]
              (grid/add-cells grid
                              {:entry-cell entry-cell
                               :update-cell #(assoc % :falling true)
                               :make-cells make-cells})))))

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

