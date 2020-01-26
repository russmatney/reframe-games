(ns games.grid.core
  (:require
   [clojure.set :as set]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Grid, row, and cell creation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn reset-cell-labels
  "Adds :x, :y keys and vals to every cell in the grid.
  Used to initialize the board, and to reset x/y after removing rows/cells.
  "
  [{:keys [phantom-rows]} grid]
  (vec
   (map-indexed
    (fn [y row]
     (vec
      (map-indexed
       (fn [x cell]
        (assoc cell :y (- y phantom-rows) :x x))
       row)))
    grid)))

(defn build-row
  "Used to create initial rows as well as new ones after rows are removed."
  [{:keys [width]}]
  (vec (take width (repeat {}))))

(defn build-grid
  "Builds a grid with the passed `opts`.
  Expects :height, :width, :phantom-rows as `int`s.

  Can also be used to rebuild/reset the grid.
  "
  [{:keys [height phantom-rows] :as opts}]
  (-> opts
    (assoc :grid
      (reset-cell-labels opts
        (take (+ height phantom-rows) (repeat (build-row opts)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Row manipulation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
  (let [cleared-grid (remove row-predicate grid)
        rows-to-add (- (+ height phantom-rows) (count cleared-grid))
        new-rows (take rows-to-add (repeat (build-row db)))
        grid-with-new-rows (concat new-rows cleared-grid)
        updated-grid (reset-cell-labels db grid-with-new-rows)]
    (assoc db :grid updated-grid)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell Predicates
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn within-bounds?
  "Returns true if the passed cell coords is within the edges of the grid."
  [{:keys [height width] :as db} {:keys [x y]}]
  (and
   (> height y)
   (> width x)
   (>= x 0)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell Prop Updates
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn update-cell
  "Applies the passed function to the cell at the specified coords."
  [{:keys [grid phantom-rows] :as db} {:keys [x y]} f]
  (let [updated (update-in grid [(+ phantom-rows y) x] f)]
      (assoc db :grid updated)))

(defn overwrite-cell
  "Copies all props from `cell` to `target`.
  Looks up the cell passed to get the latest props before copying.
  Merges any new properies included on the passed `cell`.

  Some thoughts after reading:
  not sure why a merge here, vs clearing and setting the passed props.
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

(defn add-cells
  "Adds the passed cells to the passed grid"
  [db {:keys [make-cells update-cell entry-cell]}]
  (let [update-f (or update-cell (fn [c] c))
        cells (make-cells entry-cell)
        cells (map update-cell cells)]
    (reduce
       (fn [db {:keys [x y] :as cell}]
         (overwrite-cell db {:cell cell :target {:x x :y y}}))
       db
       cells)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell Fetching
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-cell
  [{:keys [grid phantom-rows]} {:keys [x y]}]
  (-> grid (nth (+ y phantom-rows)) (nth x)))

(defn get-cells
  [{:keys [grid]} pred]
  (filter pred (flatten grid)))

(defn cell->coords
  "Returns only the coords of a cell as :x and :y
  Essentially drops the props.
  Used to compare sets of cells."
  [{:keys [x y]}] {:x x :y y})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell 'Movement'
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn move-cell-coords
  "Returns a map with :x, :y coords relative to the passed direction
  (:left, :right, :up, :down)."
  [{:keys [x y]} direction]
  (let [x-diff (case direction :left -1 :right 1 0)
        y-diff (case direction :down 1 :up -1 0)]
     {:x (+ x x-diff)
      :y (+ y y-diff)}))

(defn- calc-move-cells
  [db {:keys [cells move-f can-move?] :as move-opts}]
  (let [cells-and-targets
        (map (fn [c] {:cell (get-cell db c)
                      :target (move-f c)})
             cells)
        targets (map :target cells-and-targets)
        cells-to-move (set (map cell->coords cells))
        target-coords (set (map cell->coords targets))
        cells-to-clear (set/difference cells-to-move target-coords)
        all-can-move? (not (seq (remove can-move? targets)))]

    {:cells-and-targets cells-and-targets
     :targets targets
     :cells-to-clear cells-to-clear
     :all-can-move? all-can-move?}))

(defn move-cells
  "Moves a group of passed `cells` according to `move-f`.
  Only moves if all passed cells return true for `can-move?`.
  Otherwise, returns the db as-is.

  This function does the work of copying cell props from one cell to another,
  clearing props on cells that have been abandoned, and being smart about not
  clearing cells that are being moved into.
  "
  [db {:keys [cells move-f fallback-moves] :as move-opts}]
  (let [{:keys [cells-and-targets targets cells-to-clear all-can-move?]}
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

      true db)))
