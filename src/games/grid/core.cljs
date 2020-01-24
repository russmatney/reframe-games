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
  "
  [{:keys [height phantom-rows] :as opts}]
  (reset-cell-labels opts
    (take (+ height phantom-rows) (repeat (build-row opts)))))

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

(defn move-cells
  "Moves a group of passed `cells` according to `move-f`.
  Only moves if all passed cells return true for `can-move?`.

  This function does the work of copying cell props from one cell to another,
  clearing props on cells that have been abandoned, and being smart about not
  clearing cells that are being moved into.
  "
  [db {:keys [cells move-f can-move?]}]
  (let [cells-and-targets
        (map (fn [c] {:cell (get-cell db c)
                      :target (move-f c)})
             cells)
        targets (map :target cells-and-targets)
        cells-to-move (set (map cell->coords cells))
        target-coords (set (map cell->coords targets))
        cells-to-clear (set/difference cells-to-move target-coords)

        any-cant-move? (seq (remove can-move? targets))]

    (if any-cant-move? db
      (as-> db db
        ;; copy cells that are 'moving'
        (reduce overwrite-cell db cells-and-targets)
        ;; clear cells that were left
        (reduce clear-cell-props db cells-to-clear)))))
