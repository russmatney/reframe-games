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
  (let [style {:background "#6ebff5"}] ;; light blue
    (map #(assoc % :style style)
         [(relative ec {:x 2})
          (relative ec {:x 1})
          (assoc ec :anchor true)
          (relative ec {:x -1})])))

(defn t-shape [ec]
  (let [style {:background "#B564D4"}] ;; magenta
    (map #(assoc % :style style)
      [(relative ec {:y -1})
       (relative ec {:x -1})
       (relative ec {:x 1})
       (assoc ec :anchor true)])))

(defn z-shape [ec]
  (let [style {:background "rgb(231,110,85)"}] ;; green
    (map #(assoc % :style style)
      [(relative ec {:y -1})
       (relative ec {:y -1 :x -1})
       (relative ec {:x 1})
       (assoc ec :anchor true)])))

(defn s-shape [ec]
  (let [style {:background "#FE493C"}] ;; red
    (map #(assoc % :style style)
      [(relative ec {:y -1})
       (relative ec {:y -1 :x 1})
       (relative ec {:x -1})
       (assoc ec :anchor true)])))

(defn r-shape [ec]
  (let [style {:background "#209CEE"}] ;; blue
    (map #(assoc % :style style)
      [(relative ec {:x -1 :y -1})
       (relative ec {:x -1})
       (relative ec {:x 1})
       (assoc ec :anchor true)])))

(defn l-shape [ec]
  (let [style {:background "rgb(146,204,65)"}] ;; orange coral
    (map #(assoc % :style style)
      [(relative ec {:x 1 :y -1})
       (relative ec {:x -1})
       (relative ec {:x 1})
       (assoc ec :anchor true)])))

(defn square-shape [ec]
  (let [style {:background "rgb(247,213,29)"}] ;; yello
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
   :entry-cell {:x 5 :y -1}
   :preview-grids
   [(grid/build-grid {:height 2 :width 4})
    (grid/build-grid {:height 2 :width 4})
    (grid/build-grid {:height 2 :width 4})]
   :piece-queue [(rand-nth allowed-shape-fns)
                 (rand-nth allowed-shape-fns)
                 (rand-nth allowed-shape-fns)
                 (rand-nth allowed-shape-fns)]
   :max-queue-size 5
   :allowed-shape-fns allowed-shape-fns
   :falling-shape-fn nil
   :held-shape-fn nil
   :held-grid (grid/build-grid {:height 2 :width 4})
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
   :paused? false
   :current-view :game
   :controls {:move-left (set ["left" "h" "a"])
              :move-down (set ["down" "j" "s"])
              :move-right (set ["right" "l" "d"])
              :hold (set ["space"])
              :rotate (set ["up" "k" "w"])
              :pause (set ["enter"])
              :controls (set ["c"])
              :about (set ["b"])
              :game (set ["g"])}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Control helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def key-label->re-pressed-key
  "Maps a nice string to a re-pressed key with keyCode."
  {"enter" {:keyCode 13}
   "space" {:keyCode 32}
   "left" {:keyCode 37}
   "right" {:keyCode 39}
   "up" {:keyCode 38}
   "down" {:keyCode 40}
   "a" {:keyCode 65}
   "b" {:keyCode 66}
   "c" {:keyCode 67}
   "d" {:keyCode 68}
   "e" {:keyCode 69}
   "f" {:keyCode 70}
   "g" {:keyCode 71}
   "h" {:keyCode 72}
   "j" {:keyCode 74}
   "k" {:keyCode 75}
   "l" {:keyCode 76}
   "s" {:keyCode 83}
   "w" {:keyCode 87}})

(def supported-keys (set (keys key-label->re-pressed-key)))

(def control->label
  "Maps a control to a human label"
  {:move-left "Move Left"
   :move-right "Move Right"
   :move-down "Move Down"
   :rotate "Rotate"
   :hold "Hold / Swap"
   :pause "Pause"
   :controls "Controls"
   :about "About"
   :game "Back to Game"})
