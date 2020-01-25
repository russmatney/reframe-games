(ns games.tetris.core
  (:require
   [games.tetris.db :as tetris.db]
   [games.grid.core :as grid]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Predicates and game logic
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cell-occupied? [db cell]
  (:occupied (grid/get-cell db cell)))

(defn cell-open?
  "Returns true if the indicated cell is not occupied or beyond the edge of the
  grid."
  [{:keys [height width] :as db} {:keys [x y]}]
  (and
      (> height y)
      (> width x)
      (>= x 0)
      (not (cell-occupied? db {:x x :y y}))))

(defn can-add-new? [{:keys [entry-cell] :as db}]
  "Returns true if the entry cell is not occupied,
  or if the entry cell itself can move down.
  TODO refactor to depend on the next piece being added
  (which may cover more than just the entry-cell)
  "
  (not (cell-occupied? db entry-cell)))

(defn row-fully-occupied? [row]
   (= (count row)
      (count (filter :occupied row))))

(defn rows-to-clear?
  "Returns true if there are rows to be removed from the board."
  [{:keys [grid]}]
  (seq (filter row-fully-occupied? grid)))

(defn clear-full-rows
  [{:keys [grid height phantom-rows] :as db}]
  (let [cleared-grid (remove row-fully-occupied? grid)
        rows-to-add (- (+ height phantom-rows) (count cleared-grid))
        new-rows (take rows-to-add (repeat (grid/build-row db)))
        grid-with-new-rows (concat new-rows cleared-grid)
        updated-grid (grid/reset-cell-labels db grid-with-new-rows)]
    (assoc db :grid updated-grid)))

(defn any-falling?
  "Returns true if there is a falling cell anywhere in the grid."
  [{:keys [grid]}]
  (seq (filter :falling (flatten grid))))

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
  (grid/update-cell db cell
               #(-> %
                  (assoc :occupied true)
                  (dissoc :falling))))

(defn mark-cell-falling
  "Marks the passed cell (x, y) as `:falling true`.
  Returns an updated db."
  [db cell]
  (grid/update-cell db cell #(assoc % :falling true)))

(defn get-falling-cells [db]
  "Returns all cells with a `:falling true` prop"
  (grid/get-cells db :falling))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Move pieces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn move-piece
  "Moves a floating cell in the direction passed.
  If pieces try to move down but are blocked, they are locked in place (with an
  :occupied flag).
  "
  [{:keys [grid] :as db} direction]
  (let [falling-cells (get-falling-cells db)
        move-f #(grid/move-cell-coords % direction)

        db (grid/move-cells db
            {:move-f move-f
             :can-move? #(cell-open? db %)
             :cells falling-cells})

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
  [{:keys [grid] :as db}]
  (let [falling-cells (get-falling-cells db)
        anchor-cell (first (filter :anchor falling-cells))]

    (if-not anchor-cell
      ;; no anchor-cell, do nothing
      db
      (grid/move-cells db
        {:move-f #(calc-rotate-target anchor-cell %)
         :can-move? #(cell-open? db %)
         :cells (remove :anchor falling-cells)}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Adding new pieces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn select-new-piece
  "Selects a random new piece."
  [{:keys [allowed-shapes-f entry-cell] :as db}]
  (rand-nth (allowed-shapes-f entry-cell)))

(defn add-new-piece
  "Adds a new cell to the grid.
  Does not care if there is room to add it!
  Depends on the `new-piece-coord`.
  "
  [db]
  (let [new-cells (select-new-piece db)]
     (reduce
        (fn [db {:keys [x y] :as cell}]
          (-> db
              ;; write props to cell
              (grid/overwrite-cell {:cell cell :target {:x x :y y}})
              ;; mark falling
              (mark-cell-falling cell)))
        db
        new-cells)))

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

