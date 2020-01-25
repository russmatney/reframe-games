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

(def allowed-shape-fns
  [t-shape
   z-shape
   s-shape
   r-shape
   l-shape
   square-shape
   line-shape])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initial DB
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def shape-opts {})

(def initial-db
  {:game-grid
   (grid/build-grid {:height 13
                     :width 8
                     :phantom-rows 4})
   :preview-grid
   (grid/build-grid {:height 4
                     :width 4
                     :phantom-rows 4})
   :entry-cell {:x 3 :y 0}
   :allowed-shape-fns allowed-shape-fns})
