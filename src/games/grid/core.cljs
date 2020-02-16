(ns games.grid.core
  (:require
   [clojure.set :as set]
   [clojure.walk :as walk]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Grid, row, and cell creation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn reset-cell-labels
  "Adds :x, :y keys and vals to every cell in the grid.
  Used to initialize the board, and to reset x/y after removing rows/cells.
  "
  [{:keys [phantom-rows phantom-columns]} grid]
  (vec
    (map-indexed
      (fn [y row]
        (vec
          (map-indexed
            (fn [x cell]
              (assoc cell :y (- y phantom-rows) :x (- x phantom-columns)))
            row)))
      grid)))

(defn build-row
  "Used to create initial rows as well as new ones after rows are removed."
  [{:keys [width phantom-columns]}]
  (vec (take (+ width phantom-columns) (repeat {}))))

(defn build-grid
  "Builds a grid with the passed `opts`.
  Expects :height, :width, :phantom-rows, :phantom-columns as `int`s.

  Can also be used to rebuild/reset the grid.
  "
  [{:keys [height phantom-rows] :as opts}]
  (-> opts
      (assoc :grid
             (reset-cell-labels opts
                                (take (+ height phantom-rows)
                                      (repeat (build-row opts)))))))

(defn relative
  "Helper for creating cells relative to another, usually an entry-cell.
  Maintains the properties of the passed `cell` (2nd argument)."
  [{x0 :x y0 :y} {:keys [x y] :as cell}]
  (-> cell
      (assoc :x (+ x0 x))
      (assoc :y (+ y0 y))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Row manipulation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn only-positive-rows
  [db]
  (update db :grid
          (fn [grid]
            (filter (fn [row] (<= 0 (-> row (first) :y))) grid))))

(defn select-rows
  "Returns true if any row satisfies the passed row predicate."
  [{:keys [grid]} pred]
  (seq (filter pred grid)))

(defn true-for-row?
  "Helper for writing row-predicates. Funs the passed `f?` against every cell in
  a row, returning if the number of trues matches the number of cells."
  [row f?]
  (= (count row)
     (count (filter f? row))))

(defn remove-rows
  "Removes rows that return true for the passed `row-predicate`"
  [{:keys [grid height phantom-rows] :as db} row-predicate]
  (let [cleared-grid       (remove row-predicate grid)
        rows-to-add        (- (+ height phantom-rows) (count cleared-grid))
        new-rows           (take rows-to-add (repeat (build-row db)))
        grid-with-new-rows (concat new-rows cleared-grid)
        updated-grid       (reset-cell-labels db grid-with-new-rows)]
    (assoc db :grid updated-grid)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell Prop Updates
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn update-cell
  "Applies the passed function to the cell at the specified coords."
  [{:keys [grid phantom-rows phantom-columns] :as db} {:keys [x y]} f]
  (let [updated (update-in grid [(+ phantom-rows y) (+ phantom-columns x)] f)]
    (assoc db :grid updated)))

(defn update-cells
  "Applies the passed function to the cells that return true for pred."
  [db pred f]
  (update db :grid
          (fn [g]
            (into []
                  (map
                    (fn [row]
                      (into []
                            (map
                              (fn [cell]
                                (if (pred cell)
                                  (f cell)
                                  cell))
                              row)))
                    g)))))

(defn overwrite-cell
  "Copies all props from `cell` to `target`.
  Looks up the cell passed to get the latest props before copying.
  Merges any new properies included on the passed `cell`.

  Some thoughts after reading:
  not sure why a merge here, vs clearing and setting the passed props.
  "
  [db {:keys [cell target]}]
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

(defn add-cells
  "Adds the passed cells to the passed grid"
  [{:keys [entry-cell] :as db} {:keys [make-cells update-cell]}]
  (let [entry-cell (or entry-cell {:x 0 :y 0})
        update-f   (or update-cell (fn [c] c))
        cells      (make-cells entry-cell)
        cells      (map update-f cells)]
    (if-not cells
      db
      (reduce
        (fn [db {:keys [x y] :as cell}]
          (overwrite-cell db {:cell cell :target {:x x :y y}}))
        db
        cells))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell Fetching and Deleting
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-cell
  [{:keys [grid phantom-rows phantom-columns]} {:keys [x y]}]
  (-> grid (nth (+ y phantom-rows)) (nth (+ phantom-columns x))))

(defn get-cells
  [{:keys [grid]} pred]
  (filter pred (flatten grid)))

(defn any-cell?
  "Returns a seq of all the cells for which the passed predicate is true"
  [db pred]
  (seq (get-cells db pred)))

(defn clear-cells
  [db pred]
  (reduce (fn [db c] (clear-cell-props db c))
          db
          (get-cells db pred)))

(defn cell->coords
  "Returns only the coords of a cell as :x and :y
  Essentially drops the props.
  Used to compare sets of cells."
  [{:keys [x y]}] {:x x :y y})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell Transforms
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn deeper-y?
  [{y0 :y} {y1 :y}]
  (> y0 y1))

(defn ->deepest-by-x
  "Transforms a list of cells into a map of each x and the 'deepest' cell for
  that x in the given list.

  [{:x 1 :y 2}
   {:x 1 :y 1}
   {:x 2 :y 1}]
  =>
  {1 {:x 1 :y 2}
   2 {:x 2 :y 1}}
  "
  [cells]
  (let [cols            (vals (group-by :x cells))
        deepest-per-col (map #(first (sort deeper-y? %))
                             cols)]
    (->>
      deepest-per-col
      (group-by :x)
      (map (fn [[k v]] [k (first v)]))
      (into {}))))

(defn- adjacent?
  "True if the cells are neighboring cells.
  Determined by having the same x and y +/- 1, or same y and x +/- 1.
  TODO add no-walls x/y support!
  "
  [c0 c1]
  (let [{x0 :x y0 :y} c0
        {x1 :x y1 :y} c1]
    (or (and (= x0 x1)
             (or
               (= y0 (+ y1 1))
               (= y0 (- y1 1))))
        (and (= y0 y1)
             (or
               (= x0 (+ x1 1))
               (= x0 (- x1 1)))))))

(defn group-adjacent-cells
  "Takes a collection of cells, returns a list of cells grouped by adjacency.
  See `adjacent?`."
  [cells]
  ;; iterate over cells
  ;; first cell - create set, add all adjacent cells from group
  ;; next cell - if in first cell, add all adjacent to that group
  ;;    else, add all adjacent to new set
  ;; iterate
  (reduce
    (fn [groups cell]
      (let [in-a-set?      (seq (filter #(contains? % cell) groups))
            adjacent-cells (set (filter #(adjacent? % cell) cells))]
        (if in-a-set?
          ;; in a set, so walk and update group in-place
          ;; probably a better way to do this kind of in-place update
          (walk/walk
            (fn [group]
              (if (contains? group cell)
                (set/union group adjacent-cells)
                group))
            identity
            groups)
          ;; otherwise, add a new set to groups
          (conj groups (conj adjacent-cells cell)))))
    []
    cells))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell/Grid Predicates
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn within-bounds?
  "Returns true if the passed cell coords is within the edges of the grid."
  [{:keys [height width]} {:keys [x y]}]
  (and
    (> height y)
    (> width x)
    (>= x 0)))

(defn entry-cell-is?
  "Returns true if the passed predicate is true of the entry-cell."
  [{:keys [entry-cell] :as db} pred]
  (pred (get-cell db entry-cell)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell Movement
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn grid-min-max
  "Returns legal x/y mins and maxes for the passed grid.
  Some grids use phantom-columns, others don't allow it."
  [{:keys [phantom-columns phantom-rows width height]}]
  {:x-min (* -1 phantom-columns)
   :x-max (- width 1)
   :y-min (* -1 phantom-rows)
   :y-max (- height 1)})

(defn move-cell-coords
  "Returns a map with :x, :y coords relative to the passed direction
  (:left, :right, :up, :down)."
  ([{:keys [x y]} direction]
   (let [x-diff (case direction :left -1 :right 1 0)
         y-diff (case direction :down 1 :up -1 0)]
     {:x (+ x x-diff)
      :y (+ y y-diff)}))

  ([cell direction
    {:keys [no-walls? no-walls-x? no-walls-y? grid]}]
   (let [{:keys [x y]}                     (move-cell-coords cell direction)
         {:keys [x-min x-max y-min y-max]} (grid-min-max grid)

         x (if (or no-walls? no-walls-x?)
             (cond (< x x-min) x-max
                   (> x x-max) x-min
                   :else       x)
             x)
         y (if (or no-walls? no-walls-y?)
             (cond (< y y-min) y-max
                   (> y y-max) y-min
                   :else       y)
             y)]
     {:x x :y y})))

(defn- calc-move-cells
  [db {:keys [cells move-f can-move?]}]
  (let [cells-and-targets
        (map (fn [c] {:cell   (get-cell db c)
                      :target (move-f c)})
             cells)
        targets        (map :target cells-and-targets)
        cells-to-move  (set (map cell->coords cells))
        target-coords  (set (map cell->coords targets))
        cells-to-clear (set/difference cells-to-move target-coords)
        all-can-move?  (not (seq (remove can-move? targets)))]

    {:cells-and-targets cells-and-targets
     :targets           targets
     :cells-to-clear    cells-to-clear
     :all-can-move?     all-can-move?}))

(defn move-cells
  "Moves a group of passed `cells` according to `move-f`.
  Only moves if all passed cells return true for `can-move?`.
  Otherwise, returns the db as-is.

  This function does the work of copying cell props from one cell to another,
  clearing props on cells that have been abandoned, and being smart about not
  clearing cells that are being moved into.
  "
  [db {:keys [fallback-moves] :as move-opts}]
  (let [{:keys [cells-and-targets cells-to-clear all-can-move?]}
        (calc-move-cells db move-opts)
        {:keys [fallback-move-f additional-cells]} (first fallback-moves)]
    (cond
      all-can-move?
      (as-> db db
        ;; copy cells that are 'moving'
        (reduce overwrite-cell db cells-and-targets)
        ;; clear cells that were left
        (reduce clear-cell-props db cells-to-clear))

      fallback-move-f
      (let [fallback-moves (drop 1 fallback-moves)]
        (as-> db db
          (move-cells
            db
            (-> move-opts
                (update :cells #(concat % additional-cells))
                (assoc :move-f fallback-move-f)
                (assoc :fallback-moves fallback-moves)))))

      :else db)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell Instant Down
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; usage
;; (grid/instant-down
;;   game-grid
;;   {:cells       moveable-cells
;;    :keep-shape? true
;;    :can-move?   ;; can the cell be merged into?
;;    (fn [_] true)})

(defn instant-down
  [db {:keys [cells keep-shape? can-move?]}]
  (let []
    (println "instant-down")
    (map #(move-cell {:grid   db
                      :cell   %
                      :y-diff :until-blocked}) cells)
    db))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell rotation helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- rotate-diff
  "x1 = y0
  y1 = -x0"
  [{:keys [x y]}]
  {:x y
   :y (* -1 x)})

(defn- calc-diff [anchor-cell cell]
  {:x (- (:x anchor-cell) (:x cell))
   :y (- (:y anchor-cell) (:y cell))})

(defn- apply-diff [anchor-cell cell]
  {:x (+ (:x anchor-cell) (:x cell))
   :y (+ (:y anchor-cell) (:y cell))})

(defn calc-rotate-target
  "Rotates the passed `cell` about the `anchor-cell` clockwise.
  Returns a target cell as a map with :x and :y keys for `cell`'s new
  coordinates."
  [anchor-cell cell]
  (apply-diff anchor-cell (rotate-diff (calc-diff anchor-cell cell))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Grid rotation helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn spin
  "Reverses the grid in the x or y direction, returning a rotated grid."
  [db {:keys [reverse-x? reverse-y?]}]
  (update db :grid
          (fn [grid]
            (map (fn [row] (if reverse-x? (reverse row) row))
                 (if reverse-y? (reverse grid) grid)))))
