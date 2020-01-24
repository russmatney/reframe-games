(ns games.tetris.db
  (:require
   [games.grid.core :as grid]))

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
;; Initial DB
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def grid-opts
  "The height and width are a count for how many visible cells the board shows.
  Phantom rows help new pieces enter the board one row at a time, but adding
  extra rows above the top of the board."
  {:height 13
   :width 8
   :phantom-rows 4})

(def initial-db
  (-> grid-opts
    (assoc :grid (grid/build-grid grid-opts))
    (assoc :entry-cell {:x 3 :y 0})
    (assoc :allowed-shapes-f allowed-shapes-f)))
