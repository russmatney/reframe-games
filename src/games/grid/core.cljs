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
  (update
    db :grid
    (fn [g]
      (into
        []
        (map
          (fn [row]
            (into
              []
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
    (update-cell
      db target
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
  [{:keys [grid width height phantom-rows phantom-columns]} {:keys [x y]}]
  (let [ynth (+ y phantom-rows)
        xnth (+ x phantom-columns)]
    (if (>= y height) nil
        (-> grid (nth ynth) (nth xnth)))))

(defn get-cells
  [{:keys [grid] :as g} pred]
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

(defn same-cell?
  "True if the cells have the same coords"
  [c1 c2]
  (= (cell->coords c1) (cell->coords c2)))

(defn cell-in-group?
  [group c]
  (let [coords (set (map cell->coords group))]
    (contains? coords (cell->coords c))))

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
  ([db cell] (within-bounds? db cell {:allow-above? false}))
  ([{:keys [height width]} {:keys [x y]} {:keys [allow-above?]}]
   (and
     (> height y)
     (> width x)
     (or allow-above? (>= y 0))
     (>= x 0))))

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
  (:left, :right, :up, :down).

  Also accepts a third `game-opts` option, which it uses to support
  moving across walls in `no-walls?` `no-walls-x?` and `no-walls-y?` modes"
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
                      :target (get-cell db (move-f c))})
             cells)
        targets        (map :target cells-and-targets)
        cells-to-move  (set (map cell->coords cells))
        target-coords  (set (map cell->coords targets))
        cells-to-clear (set/difference cells-to-move target-coords)
        all-can-move?  (and (not-any? nil? targets)
                            (not (seq (remove can-move? targets))))]

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
;; Cell relative movement/distance and rotation helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn rotate-diff
  "x1 = y0
  y1 = -x0"
  [{:keys [x y]}]
  {:x y
   :y (* -1 x)})

(defn calc-diff [cell-a cell-b]
  {:x (- (:x cell-a) (:x cell-b))
   :y (- (:y cell-a) (:y cell-b))})

(defn apply-diff [cell-a cell-b]
  {:x (+ (:x cell-a) (:x cell-b))
   :y (+ (:y cell-a) (:y cell-b))})

(defn calc-rotate-target
  "Rotates the passed `cell` about the `anchor-cell` clockwise.
  Returns a target cell as a map with :x and :y keys for `cell`'s new
  coordinates."
  [anchor-cell cell]
  (apply-diff anchor-cell (rotate-diff (calc-diff anchor-cell cell))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell Instant Down
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn greater-x? [{x1 :x} {x2 :x}] (> x1 x2))
(defn less-x? [{x1 :x} {x2 :x}] (< x1 x2))
(defn same-x? [{x1 :x} {x2 :x}] (= x1 x2))

(defn greater-y? [{y1 :y} {y2 :y}] (> y1 y2))
(defn less-y? [{y1 :y} {y2 :y}] (< y1 y2))
(defn same-y? [{y1 :y} {y2 :y}] (= y1 y2))

(defn get-column-or-row [db cell direction]
  (get-cells
    db
    (fn [c]
      (case direction
        (:up :down)    (same-x? cell c)
        (:left :right) (same-y? cell c)))))

(defn cells-in-direction
  "Returns the cells in the passed direction from the given cell."
  [db cell direction]
  (let [col-or-row-cells (get-column-or-row db cell direction)]
    (filter #(case direction
               :up    (less-y? % cell)
               :down  (greater-y? % cell)
               :right (greater-x? % cell)
               :left  (less-x? % cell))
            col-or-row-cells)))


(defn cell->furthest-open-space
  "Returns the open cell that is furthest in `direction`.

  furthest _consecutive_ open space

  Currently walks in that direction until a cell is not open. Could be expanded
  to allow 'skipping' but that behavior doesn't fit a tetris/puyo game that i've
  played, so, leaving this for now.
  "
  [db cell {:keys [can-move? direction]}]
  (let [cells-in-dir (cells-in-direction db cell direction)
        sorted       (sort-by
                       (case direction
                         (:up :down)    :y
                         (:left :right) :x)
                       (case direction
                         (:up :left)    >
                         (:down :right) <)
                       cells-in-dir)
        target       (:best
                      (reduce (fn [{:keys [best skip?]} next-cell]
                                (cond
                                  skip?
                                  {:skip? true
                                   :best  best}

                                  (and
                                    ;; nothing set and can't move already?
                                    ;; we're blocked, skip and return nil
                                    (not (can-move? next-cell))
                                    (not best))
                                  {:skip? true
                                   :best  nil}

                                  (can-move? next-cell)
                                  {:best  next-cell
                                   :skip? false}

                                  :else
                                  {:best  best
                                   :skip? true}))
                              {:best  nil
                               :skip? false}
                              sorted))]
    (when (seq cells-in-dir)
      target)))

(defn ->cell-and-target
  "Derp, baked in this furthest open space logic."
  [db c move-opts]
  {:cell   c
   :target (cell->furthest-open-space
             db c move-opts)})

(defn ->diff-and-magnitude
  [{:keys [cell target] :as c-n-t}]
  (let [{dx :x dy :y :as diff} ;; document the negative diff here
        (calc-diff target cell)]
    (assoc c-n-t
           :magnitude
           (apply max (map #(.abs js/Math %) [dx dy]))
           :diff diff)))

(defn instant-fall
  "Returns a `db` with the passed cells moving as far in the passed `direction`
  as possible.

  If `keep-shape?` is true, the shortest available move will be made for all
  passed cells. If it is false, cells will fall independently until they reach
  the furthest open space.

  See furthest open space for definition/note on consecutive spaces.
  "
  [db {:keys [cells keep-shape? can-move? direction]
       :as   move-opts}]
  (if-not keep-shape?

    (let [sorted-cells (sort-by
                         (case direction
                           (:up :down)    :y
                           (:left :right) :x)
                         (case direction
                           (:up :left)    <
                           (:down :right) >)
                         cells)]
      ;; order is important - go from the direction passed
      ;; (:down -> start from bottom of grid (decreasing y))
      (reduce
        (fn [db cell]
          (let [{:keys [diff target]}
                (->diff-and-magnitude
                  (->cell-and-target db cell move-opts))]
            (if-not target
              db
              (move-cells
                db
                {:cells     [cell]
                 :move-f    #(apply-diff % diff)
                 :can-move? can-move?})))
          ) db sorted-cells))

    (let [c-n-ts   (map #(->cell-and-target db % move-opts) cells)
          c-n-ts   (filter :target c-n-ts)
          c-n-ts   (map ->diff-and-magnitude c-n-ts)
          shortest (first (sort-by :magnitude < c-n-ts))]
      (println "shortest " shortest)
      (println "c-n-ts " c-n-ts)
      (println "cells " cells)
      (if (can-move? (:target shortest))
        (move-cells db
                    {:cells     cells
                     :move-f    #(apply-diff % (:diff shortest))
                     :can-move? can-move?})
        db)
      )))

(comment
  (.abs js/Math -1)

  (sort-by :x < [{:x 2} {:x 1}]))

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
