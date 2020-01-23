(ns toying.tetris.db)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Piece Shapes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO note that these don't yet depend on the entry-cell in the db, so are not
;; dynamic
(def entry-cell {:x 3 :y 0})
(def single-cell-shape [entry-cell])
(def three-cell-shape [{:x 1 :y 0}
                       {:x 2 :y 0 :anchor true}
                       entry-cell])
(def two-cell-shape [{:x 3 :y -1 :anchor true}
                     entry-cell])
(def angle-shape [{:x 2 :y 0}
                  (assoc entry-cell :anchor true)
                  {:x 3 :y -1}])

(def allowed-shapes
  [single-cell-shape
   two-cell-shape
   angle-shape
   three-cell-shape])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Building the Grid
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

(defn- build-grid [{:keys [height phantom-rows] :as db}]
  (reset-cell-labels db
    (take (+ height phantom-rows) (repeat (build-row db)))))

;; TODO move grid stuff to grid namespace (db, etc)
(def grid-opts
  "The height and width are a count for how many visible cells the board shows.
  Phantom rows help new pieces enter the board one row at a time, but adding
  extra rows above the top of the board."
  {:height 6
   :width 7
   :phantom-rows 4})

(def initial-db
  (-> grid-opts
    (assoc :grid (build-grid grid-opts))
    (assoc :entry-cell entry-cell)
    (assoc :allowed-shapes allowed-shapes)))
