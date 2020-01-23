(ns toying.tetris
  (:require
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [clojure.set :as set]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game constants

(def grid-phantom-rows 4)
(def grid-height 6)
(def grid-width 7)

(def new-piece-coord {:x 3 :y 0})
(def single-cell-shape [new-piece-coord])
(def three-cell-shape [{:x 1 :y 0}
                       {:x 2 :y 0 :anchor true}
                       {:x 3 :y 0}])
(def two-cell-shape [{:x 3 :y -1 :anchor true}
                     {:x 3 :y 0}])
(def angle-shape [{:x 2 :y 0}
                  {:x 3 :y 0 :anchor true}
                  {:x 3 :y -1}])

(def allowed-shapes
  [single-cell-shape
   two-cell-shape
   angle-shape
   three-cell-shape])

(defn reset-cell-labels [grid]
  (vec
   (map-indexed
    (fn [y row]
     (vec
      (map-indexed
       (fn [x cell]
        (assoc cell :y (- y grid-phantom-rows) :x x))
       row)))
    grid)))

(defn build-row []
  (vec (take grid-width (repeat {}))))

(defn build-grid []
  (reset-cell-labels
    (take (+ grid-height grid-phantom-rows) (repeat (build-row)))))

(def initial-game-state
  {:grid (build-grid)
   :phase :falling})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Updating cells

(defn update-cell
  "Applies the passed function to the cell at the specified coords."
  ([db cell f]
   (update-cell db (:x cell) (:y cell) f))
  ([db x y f]
   (let [grid (-> db :game-state :grid)
         updated (update-in grid [(+ grid-phantom-rows y) x] f)]
      (assoc-in db [:game-state :grid] updated))))

(defn mark-cell-occupied
  "Marks the passed cell (x, y) as occupied, dissoc-ing the :falling key.
  Returns an updated db."
  ([db {:keys [x y]}]
   (update-cell db x y
               #(-> %
                  (assoc :occupied true)
                  (dissoc :falling)))))

(defn mark-cell-falling
  "Marks the passed cell (x, y) as falling.
  Returns an updated db."
  ([db {:keys [x y]}]
   (update-cell db x y #(assoc % :falling true))))

(defn unmark-cell-falling
  "Removes :falling key from the passed cell (x, y).
  Returns an updated db."
  ([db {:keys [x y]}]
   (unmark-cell-falling db x y))
  ([db x y]
   (update-cell db x y #(dissoc % :falling))))

(defn get-cell
  ([grid {:keys [x y]}]
   (get-cell grid x y))
  ([grid x y]
   (-> grid (nth (+ y grid-phantom-rows)) (nth x))))

(defn overwrite-cell
  "Copies all props from `cell` to `target`.
  Looks up the cell in passed using coords to get the latest props before
  copying.
  Merges any new properies included on the passed `cell`.
  "
  [db {:keys [cell target]}]
  (let [grid (-> db :game-state :grid)
        props (dissoc cell :x :y)]
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
;; Predicates and their helpers

(defn cell-occupied? [cell]
   (:occupied cell))

(defn can-move?
  "Returns true if the indicated cell is not occupied or beyond the edge of the
  grid."
  ([grid cell]
   (can-move? grid cell nil))
  ([grid {:keys [x y]} direction]
   (let [next-x (+ (case direction :right 1 :left -1 0) x)
         next-y (+ (case direction :down 1 0) y)]
     (and
      (> grid-height next-y)
      (> grid-width next-x)
      (>= next-x 0)
      (not (cell-occupied? (get-cell grid next-x next-y)))))))

(defn can-add-new? [grid]
  (let [{:keys [y x]} new-piece-coord
        entry-cell (get-cell grid x y)]
    (or
     (not (cell-occupied? entry-cell))
     (can-move? grid entry-cell :down))))

(defn row-fully-occupied? [row]
   (= (count row)
      (count (seq (filter :occupied row)))))

(defn rows-to-clear?
  "Returns true if there are rows to be removed from the board."
  [db]
  (let [grid (-> db :game-state :grid)]
    (seq (filter row-fully-occupied? grid))))

(defn clear-full-rows [db]
  (let [grid (-> db :game-state :grid)
        cleared-grid (seq (remove row-fully-occupied? grid))
        rows-to-add (- (+ grid-height grid-phantom-rows) (count cleared-grid))
        new-rows (take rows-to-add (repeat (build-row)))
        grid-with-new-rows (concat new-rows cleared-grid)
        updated-grid (reset-cell-labels grid-with-new-rows)]
    (assoc-in db [:game-state :grid] updated-grid)))

(defn any-falling?
  "Returns true if there is a falling cell anywhere in the grid."
  [db]
  (let [grid (-> db :game-state :grid)
        all-cells (flatten grid)]
   (seq (filter :falling all-cells))))

(defn gameover?
  "Returns true if no new pieces can be added.
  Needs to know what the next piece is, doesn't it?
  TODO generate list of pieces to be added, pull from the db here
  "
  [db]
  (let [grid (-> db :game-state :grid)]
   (not (can-add-new? grid))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Moving pieces and cells
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-falling-cells [db]
  (let [grid (-> db :game-state :grid)
        all-cells (flatten grid)]
    (seq (filter :falling all-cells))))

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
  [db direction]
  (let [grid (-> db :game-state :grid)
        falling-cells (get-falling-cells db)
        current-cells (set (map (fn [{:keys [x y]}]
                                   {:x x :y y})
                                falling-cells))
        next-cells (set (map #(move-cell % direction) current-cells))
        cells-to-clear (set/difference current-cells next-cells)

        currents-and-targets (map
                              (fn [c] {:cell (get-cell grid c)
                                       :target (move-cell c direction)})
                              current-cells)]
    (cond
      ;; all falling pieces can move
      (empty? (seq (remove #(can-move? grid % direction) falling-cells)))
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
       (seq (remove #(can-move? grid % :down) falling-cells)))
      ;; mark all cells
      (reduce (fn [db cell] (mark-cell-occupied db cell))
              db falling-cells)

      ;; otherwise just return the db
      true db)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Adding new pieces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn select-new-piece []
  (rand-nth allowed-shapes))

(defn add-new-piece
  "Adds a new cell to the grid.
  Does not care if there is room to add it!
  Depends on the `new-piece-coord`.
  "
  [db]
  (let [new-cells (select-new-piece)]
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
  [db]
  (let [grid (-> db :game-state :grid)
        falling-cells (get-falling-cells db)
        anchor-cell (first (filter :anchor falling-cells))]

    (cond
      ;; no anchor-cell, do nothing
      (not anchor-cell)
      db

     true
     (let [to-rotate (seq (remove :anchor falling-cells))
           vals (map (fn [c] {:cell c
                              :target (calc-rotate-target anchor-cell c)})
                     to-rotate)
           new-cells (set (map (fn [{:keys [target]}] (cell->coords target))
                               vals))
           old-cells (set (map cell->coords to-rotate))
           cells-to-clear
           (set/difference old-cells new-cells)

           any-cant-move? (seq (remove (fn [c]
                                         (can-move? grid c))
                                       new-cells))]
       (cond
         ;; at least one dest cell is not allowed
         (seq (remove #(can-move? grid %) new-cells))
         db

         true
         (as-> db db
           (reduce overwrite-cell db vals)
           (reduce clear-cell-props db cells-to-clear)))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game tick/steps functions

(defn step-falling [db]
  (cond
    ;; clear pieces, update db and return
    (rows-to-clear? db) ;; animation?
    (clear-full-rows db)

    ;; game is over, update db and return
    ;;(gameover? db) (assoc db :gameover true) ;; or :phase :gameover?
    (gameover? db) (assoc db :game-state initial-game-state)

    ;; a piece is falling, move it down
    (any-falling? db)
    (move-piece db :down)

    ;; nothing is falling, add a new piece
    (not (any-falling? db))
    (add-new-piece db)

    ;; do nothing
    true db))

(defn step [db]
  (let [phase (:game-phase db)]
    (case phase
      :falling (step-falling db)
      nil (step-falling db))))

