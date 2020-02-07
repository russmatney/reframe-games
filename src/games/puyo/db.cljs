(ns games.puyo.db
  (:require
   [games.grid.core :as grid]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Piece Types
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def colors
  [:red
   :green
   :yellow
   :blue])

(defn build-piece-fn []
  (let [colorA (rand-nth colors)
        colorB (rand-nth colors)]
    (fn [{x :x y :y}]
      [{:x x :y y :anchor true :color colorA}
       {:x x :y (- y 1) :color colorB}])))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initial DB
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def initial-controls
  {:move-left  {:label "Move Left"
                :keys  (set ["left" "h" "a"])
                :event [:games.puyo.events/move-piece :left]}
   :move-down  {:label "Move Down"
                :keys  (set ["down" "j" "s"])
                :event [:games.puyo.events/move-piece :down]}
   :move-right {:label "Move Right"
                :keys  (set ["right" "l" "d"])
                :event [:games.puyo.events/move-piece :right]}
   :hold       {:label "Hold"
                :keys  (set ["space"])
                :event [:games.puyo.events/hold-and-swap-piece]}
   :rotate     {:label "Rotate Piece"
                :keys  (set ["up" "k" "w"])
                :event [:games.puyo.events/rotate-piece]}
   :pause      {:label "Pause"
                :keys  (set ["enter"])
                :event [:games.puyo.events/toggle-pause]}
   :controls   {:label "Controls"
                :keys  (set ["c"])
                :event [:games.puyo.events/set-view :controls]}
   :about      {:label "About"
                :keys  (set ["b"])
                :event [:games.puyo.events/set-view :about]}
   :game       {:label "Return to game"
                :keys  (set ["g"])
                :event [:games.puyo.events/set-view :game]}})

(def show-grid (grid/build-grid {:height 2 :width 1 :entry-cell {:x 0 :y 1}}))

(def initial-db
  { ;; game (matrix)
   :game-grid
   (grid/build-grid {:height       10
                     :width        8
                     :phantom-rows 2
                     :entry-cell   {:x 4 :y -1}})

   ;; game logic
   :current-view      :game
   :group-size        4 ;; number of puyos in a group to be removed
   :tick-timeout      300
   :paused?           false
   :gameover?         false
   :waiting-for-fall? false
   :pieces-played     0

   ;; queue
   :piece-queue    (repeatedly 5 build-piece-fn)
   :min-queue-size 5
   :preview-grids  (repeat 3 show-grid)

   ;; controls
   :controls initial-controls

   ;; timer
   :time      0
   :timer-inc 100

   ;; hold/swap
   :falling-shape-fn nil
   :held-shape-fn    nil
   :held-grid        show-grid
   :hold-lock        false

   ;; modes
   :spin-the-bottle? true

   ;; level/score
   :level                 1
   :groups-per-level      5
   :groups-cleared        0
   :score                 0
   :score-per-group-clear 10
   :groups-in-combo       0
   :last-combo-piece-num  nil})


