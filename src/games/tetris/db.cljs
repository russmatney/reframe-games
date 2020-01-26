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
  (let [style {:background "powderblue"}]
    (map #(assoc % :style style)
      [(relative ec {:y -3})
       (relative ec {:y -2})
       (relative ec {:y -1 :anchor true})
       ec])))

(defn t-shape [ec]
  (let [style {:background "coral"}]
    (map #(assoc % :style style)
      [(relative ec {:y -1 :anchor true})
       (relative ec {:y -1 :x -1})
       (relative ec {:y -1 :x 1})
       ec])))

(defn z-shape [ec]
  (let [style {:background "red"}]
    (map #(assoc % :style style)
      [(relative ec {:y -1 :anchor true})
       (relative ec {:y -1 :x -1})
       (relative ec {:y -2 :x -1})
       ec])))

(defn s-shape [ec]
  (let [style {:background "green"}]
    (map #(assoc % :style style)
      [(relative ec {:y -1 :anchor true})
       (relative ec {:y -1 :x 1})
       (relative ec {:y -2 :x 1})
       ec])))

(defn r-shape [ec]
  (let [style {:background "blue"}]
    (map #(assoc % :style style)
      [(relative ec {:y -1 :anchor true})
       (relative ec {:y -2})
       (relative ec {:y -2 :x 1})
       ec])))

(defn l-shape [ec]
  (let [style {:background "orange"}]
    (map #(assoc % :style style)
      [(relative ec {:y -1 :anchor true})
       (relative ec {:y -2})
       (relative ec {:y -2 :x -1})
       ec])))

(defn square-shape [ec]
  (let [style {:background "yellow"}]
    (map #(assoc % :style style)
      [(relative ec {:y -1 :anchor true})
       (relative ec {:x -1})
       (relative ec {:y -1 :x -1})
       ec])))

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
   (grid/build-grid {:height 5
                     :width 5})
   :piece-queue [(rand-nth allowed-shape-fns)]
   :entry-cell {:x 3 :y 0}
   :allowed-shape-fns allowed-shape-fns
   :ticks 0
   :tick-timeout 500
   :time 0
   :timer-inc 100
   :level 1
   :pieces-per-level 3
   :pieces-played 0
   :score 0})
