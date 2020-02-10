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

;; TODO come off the db, or operate over passed colors or the db itself
(defn build-piece-fn []
  (let [colorA (rand-nth colors)
        colorB (rand-nth colors)]
    (fn [{x :x y :y}]
      [{:x x :y y :anchor true :color colorA}
       {:x x :y (- y 1) :color colorB}])))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initial DB
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn initial-controls
  [game-opts]
  {:move-left  {:label "Move Left"
                :keys  (set ["left" "h" "a"])
                :event [:games.puyo.events/move-piece game-opts :left]}
   :move-down  {:label "Move Down"
                :keys  (set ["down" "j" "s"])
                :event [:games.puyo.events/move-piece game-opts :down]}
   :move-right {:label "Move Right"
                :keys  (set ["right" "l" "d"])
                :event [:games.puyo.events/move-piece game-opts :right]}
   :hold       {:label "Hold"
                :keys  (set ["space"])
                :event [:games.puyo.events/hold-and-swap-piece game-opts]}
   :rotate     {:label "Rotate"
                :keys  (set ["up" "k" "w"])
                :event [:games.puyo.events/rotate-piece game-opts]}
   :pause      {:label "Pause"
                :keys  (set ["enter"])
                :event [:games.puyo.events/toggle-pause game-opts]}})

(def piece-grid (grid/build-grid {:height     2
                                  :width      1
                                  :entry-cell {:x 0 :y 1}}))

(def defaults
  {:step-timeout    500
   :ignore-controls false})

(defn initial-db
  "Creates an initial puyo game state."
  ([] (initial-db {:name :default}))

  ([game-opts]
   (let [{:keys [name game-grid step-timeout ignore-controls] :as game-opts}
         (merge defaults game-opts)]
     {:name      name
      :game-opts game-opts

      ;; game (matrix)
      :game-grid
      (grid/build-grid
        (merge
          {:height       10
           :width        10
           :phantom-rows 2
           :entry-cell   {:x 4 :y -1}}
          game-grid))

      ;; game logic
      :group-size        4 ;; number of puyos in a group to be removed
      :step-timeout      step-timeout
      :paused?           false
      :gameover?         false
      :waiting-for-fall? false
      :current-piece-num 0

      ;; timer
      :time 0

      ;; queue
      :piece-queue    (repeatedly 5 build-piece-fn)
      :min-queue-size 5
      :preview-grids  (repeat 3 piece-grid)

      ;; controls
      :controls        (initial-controls game-opts)
      :ignore-controls ignore-controls

      ;; hold/swap
      :falling-shape-fn nil
      :held-shape-fn    nil
      :held-grid        piece-grid
      :hold-lock        false

      ;; modes
      :spin-the-bottle? false ;; rotate the board on every piece
      :pacman-sides     true  ;; travel across the walls
      :fancy-pants      false ;; travel between games
      :mirror-mode      false ;; reverse left/right

      ;; level/score
      :level                 1
      :groups-per-level      5
      :groups-cleared        0
      :score                 0
      :score-per-group-clear 10
      :groups-in-combo       0
      :last-combo-piece-num  nil})))


