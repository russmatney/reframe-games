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

;; TODO refactor style/color into view?
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
  (let [style {:background "rgb(231,110,85)"}] ;; orange/red
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
  (let [style {:background "rgb(247,213,29)"}] ;; yellow
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

(def initial-controls
  {:move-left  {:label "Move Left"
                :keys  (set ["left" "h" "a"])
                :event [:games.tetris.events/move-piece :left]}
   :move-down  {:label "Move Down"
                :keys  (set ["down" "j" "s"])
                :event [:games.tetris.events/move-piece :down]}
   :move-right {:label "Move Right"
                :keys  (set ["right" "l" "d"])
                :event [:games.tetris.events/move-piece :right]}
   :hold       {:label "Hold"
                :keys  (set ["space"])
                :event [:games.tetris.events/hold-and-swap-piece]}
   :rotate     {:label "Rotate Piece"
                :keys  (set ["up" "k" "w"])
                :event [:games.tetris.events/rotate-piece]}
   :pause      {:label "Pause"
                :keys  (set ["enter"])
                :event [:games.tetris.events/toggle-pause]}
   :controls   {:label "Controls"
                :keys  (set ["c"])
                :event [:games.tetris.events/set-view :controls]}
   :about      {:label "About"
                :keys  (set ["b"])
                :event [:games.tetris.events/set-view :about]}
   :game       {:label "Return to game"
                :keys  (set ["g"])
                :event [:games.tetris.events/set-view :game]}})

(def initial-db
  {;; game matrix
   :game-grid
   (grid/build-grid {:height       20
                     :width        10
                     :phantom-rows 4})
   :entry-cell {:x 5 :y -1}

   ;; game logic
   :tick-timeout 500
   :paused?      false
   :gameover?    false
   :current-view :game

   ;; queue
   :piece-queue       (shuffle allowed-shape-fns)
   :min-queue-size    5
   :allowed-shape-fns allowed-shape-fns
   :preview-grids
   [(grid/build-grid {:height 2 :width 4})
    (grid/build-grid {:height 2 :width 4})
    (grid/build-grid {:height 2 :width 4})]

   ;; controls
   :controls initial-controls

   ;; hold/swap
   :falling-shape-fn nil
   :held-shape-fn    nil
   :held-grid        (grid/build-grid {:height 2 :width 4})
   :hold-lock        false

   ;; timer
   :time      0
   :timer-inc 100

   ;; level/score
   :level                1
   :rows-per-level       5
   :rows-cleared         0
   :pieces-played        0
   :score                0
   :score-per-row-clear  10
   :rows-in-combo        0
   :last-combo-piece-num nil
   })

