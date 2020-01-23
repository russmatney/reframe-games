(ns toying.tetris.core
  (:require
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [clojure.set :as set]
   [toying.tetris.db :as tetris.db]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Updating cells
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn update-cell
  "Applies the passed function to the cell at the specified coords."
  [{:keys [grid phantom-rows] :as db} {:keys [x y]} f]
  (let [updated (update-in grid [(+ phantom-rows y) x] f)]
      (assoc db :grid updated)))

(defn mark-cell-occupied
  "Marks the passed cell (x, y) as occupied, dissoc-ing the :falling key.
  Returns an updated db."
  [db cell]
  (update-cell db cell
               #(-> %
                  (assoc :occupied true)
                  (dissoc :falling))))

(defn mark-cell-falling
  "Marks the passed cell (x, y) as falling.
  Returns an updated db."
  [db cell]
  (update-cell db cell #(assoc % :falling true)))

(defn get-cell
  [{:keys [grid phantom-rows]} {:keys [x y]}]
  (-> grid (nth (+ y phantom-rows)) (nth x)))

(defn overwrite-cell
  "Copies all props from `cell` to `target`.
  Looks up the cell in passed using coords to get the latest props before
  copying.
  Merges any new properies included on the passed `cell`.
  "
  [{:keys [grid] :as db} {:keys [cell target]}]
  (let [props (dissoc cell :x :y)]
    (update-cell db target
                 (fn [target]
                   (merge
                    props
                    {:x (:x target)
                     :y (:y target)})))))

(defn clear-cell-props
  "Removes non-coordinate flags from cells."
  [db cell]
  (update-cell db cell
               (fn [c]
                 {:x (:x c)
                  :y (:y c)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Predicates
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cell-occupied? [db cell]
  (:occupied (get-cell db cell)))

(defn can-move?
  "Returns true if the indicated cell is not occupied or beyond the edge of the
  grid."
  ([db cell]
   (can-move? db cell nil))
  ([{:keys [height width] :as db} {:keys [x y]} direction]
   (let [next-x (+ (case direction :right 1 :left -1 0) x)
         next-y (+ (case direction :down 1 0) y)]
     (and
      (> height next-y)
      (> width next-x)
      (>= next-x 0)
      (not (cell-occupied? db {:x next-x :y next-y}))))))

(defn can-add-new? [{:keys [entry-cell] :as db}]
  "Returns true if the entry cell is not occupied,
  or if the entry cell itself can move down."
  (not (cell-occupied? db entry-cell)))

(defn row-fully-occupied? [row]
   (= (count row)
      (count (filter :occupied row))))

(defn rows-to-clear?
  "Returns true if there are rows to be removed from the board."
  [db]
  (seq (filter row-fully-occupied? db)))

(defn clear-full-rows [{:keys [grid height phantom-rows] :as db}]
  (let [cleared-grid (remove row-fully-occupied? grid)
        rows-to-add (- (+ height phantom-rows) (count cleared-grid))
        new-rows (take rows-to-add (repeat (tetris.db/build-row db)))
        grid-with-new-rows (concat new-rows cleared-grid)
        updated-grid (tetris.db/reset-cell-labels db grid-with-new-rows)]
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


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Moving pieces and cells
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-falling-cells
  [{:keys [grid]}]
  (filter :falling (flatten grid)))

(defn move-cell [{:keys [x y]} direction]
  (let [x-diff (case direction :left -1 :right 1 0)
        y-diff (case direction :down 1 0)]
     {:x (+ x x-diff)
      :y (+ y y-diff)}))

;; TODO consider move-cell and cell/grid apis
;; could simplify all this
(defn move-piece
  "Moves a floating cell in the direction passed.
  If pieces try to move down but are blocked, they are locked in place (with an
  :occupied flag).
  "
  [{:keys [grid] :as db} direction]
  (let [falling-cells (get-falling-cells db)
        current-cells (set (map (fn [{:keys [x y]}]
                                   {:x x :y y})
                                falling-cells))
        next-cells (set (map #(move-cell % direction) current-cells))
        cells-to-clear (set/difference current-cells next-cells)

        currents-and-targets (map
                              (fn [c] {:cell (get-cell db c)
                                       :target (move-cell c direction)})
                              current-cells)]
    (cond
      ;; all falling pieces can move
      (empty? (seq (remove #(can-move? db % direction) falling-cells)))
      ;; move all falling pieces in direction
      (as-> db db
        ;; copy cells that are 'moving'
        (reduce overwrite-cell db currents-and-targets)
        ;; clear cells that were left
        (reduce clear-cell-props db cells-to-clear))

      ;; if we try to move down but we can't, lock all falling pieces in place
      (and
       ;; down-only
       (= direction :down)
       ;; any falling cells?
       falling-cells
       ;; any falling cells that can't move down?
       ;; i.e. with occupied cells below them
       (seq (remove #(can-move? db % :down) falling-cells)))
      ;; mark all cells
      (reduce (fn [db cell] (mark-cell-occupied db cell))
              db falling-cells)

      ;; otherwise just return the db
      true db)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Adding new pieces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn select-new-piece
  "Selects a random new piece."
  [{:keys [allowed-shapes] :as db}]
  (rand-nth allowed-shapes))

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
              (overwrite-cell {:cell cell :target {:x x :y y}})
              ;; mark falling
              (mark-cell-falling cell)))
        db
        new-cells)))

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

(defn cell->coords
  "Returns only the coords of a cell as :x and :y
  Essentially drops the props.
  Used to compare sets of cells."
  [{:keys [x y]}] {:x x :y y})

(defn rotate-piece
  "Rotates a falling piece in-place.
  This requires one falling cell to be an 'anchor'.
  The anchor stays in place - the other cells calc an x/y delta between
  themselves and the anchor.

  Maybe this rule works:
  x1 = y0
  y1 = -x0
  "
  [{:keys [grid] :as db}]
  (let [falling-cells (get-falling-cells db)
        anchor-cell (first (filter :anchor falling-cells))]

    (cond
      ;; no anchor-cell, do nothing
      (not anchor-cell)
      db

     true
     (let [to-rotate (remove :anchor falling-cells)
           vals (map (fn [c] {:cell c
                              :target (calc-rotate-target anchor-cell c)})
                     to-rotate)
           new-cells (set (map (fn [{:keys [target]}] (cell->coords target))
                               vals))
           old-cells (set (map cell->coords to-rotate))
           cells-to-clear
           (set/difference old-cells new-cells)

           any-cant-move? (seq (remove (fn [c]
                                         (can-move? db c))
                                       new-cells))]
       (cond
         ;; at least one dest cell is not allowed, return db
         any-cant-move? db

         true
         (as-> db db
           (reduce overwrite-cell db vals)
           (reduce clear-cell-props db cells-to-clear)))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game tick/steps functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn step-falling [db]
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

(defn step [db]
  (step-falling db))
