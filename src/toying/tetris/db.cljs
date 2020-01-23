(ns toying.tetris.db)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Piece Shapes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn single-cell-shape [entry-cell]
  [entry-cell])

(defn relative [{x0 :x y0 :y} {:keys [x y] :as cell}]
  (-> cell
    (assoc :x (+ x0 x))
    (assoc :y (+ y0 y))))

(defn line-shape [ec]
  [(relative ec {:y -3})
   (relative ec {:y -2})
   (relative ec {:y -1 :anchor true})
   ec])

(defn t-shape [ec]
  [(relative ec {:y -1 :anchor true})
   (relative ec {:y -1 :x -1})
   (relative ec {:y -1 :x 1})
   ec])

(defn z-shape [ec]
  [(relative ec {:y -1 :anchor true})
   (relative ec {:y -1 :x -1})
   (relative ec {:y -2 :x -1})
   ec])

(defn s-shape [ec]
  [(relative ec {:y -1 :anchor true})
   (relative ec {:y -1 :x 1})
   (relative ec {:y -2 :x 1})
   ec])

(defn r-shape [ec]
  [(relative ec {:y -1 :anchor true})
   (relative ec {:y -2})
   (relative ec {:y -2 :x 1})
   ec])

(defn l-shape [ec]
  [(relative ec {:y -1 :anchor true})
   (relative ec {:y -2})
   (relative ec {:y -2 :x -1})
   ec])

(defn square-shape [ec]
  [(relative ec {:y -1 :anchor true})
   (relative ec {:x -1})
   (relative ec {:y -1 :x -1})
   ec])

(defn allowed-shapes-f [ec]
  [(t-shape ec)
   (z-shape ec)
   (s-shape ec)
   (r-shape ec)
   (l-shape ec)
   (square-shape ec)
   (line-shape ec)])

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
  {:height 12
   :width 8
   :phantom-rows 4})

(def initial-db
  (-> grid-opts
    (assoc :grid (build-grid grid-opts))
    (assoc :entry-cell {:x 3 :y 0})
    (assoc :allowed-shapes-f allowed-shapes-f)))
