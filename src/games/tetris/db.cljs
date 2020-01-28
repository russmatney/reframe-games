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
      [(relative ec {:y -1})
       (relative ec {:x 1})
       (relative ec {:y -1 :x 1})
       (assoc ec :anchor true)])))

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
   (grid/build-grid {:height 20
                     :width 10
                     :phantom-rows 4})
   :entry-cell {:x 5 :y 0}
   :preview-grids
   [(grid/build-grid {:height 4 :width 4})
    (grid/build-grid {:height 4 :width 4})
    (grid/build-grid {:height 4 :width 4})]
   :piece-queue [(rand-nth allowed-shape-fns)
                 (rand-nth allowed-shape-fns)
                 (rand-nth allowed-shape-fns)
                 (rand-nth allowed-shape-fns)]
   :max-queue-size 5
   :allowed-shape-fns allowed-shape-fns
   :falling-shape-fn nil
   :held-shape-fn nil
   :held-grid (grid/build-grid {:height 4 :width 4})
   :hold-lock false
   :ticks 0
   :tick-timeout 500
   :time 0
   :timer-inc 100
   :level 1
   :pieces-per-level 10
   :pieces-played 0
   :score 0
   :score-per-row-clear 10
   :rows-in-combo 0
   :last-combo-piece-num nil
   :paused? false})
