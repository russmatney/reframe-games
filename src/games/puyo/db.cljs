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

(defn initial-controls
  [{:keys [name] :as game-opts}]
  {:move-left  {:label "Move Left"
                :keys  (set ["left" "h" "a"])
                :event [:games.puyo.events/move-piece name :left]}
   :move-down  {:label "Move Down"
                :keys  (set ["down" "j" "s"])
                :event [:games.puyo.events/move-piece name :down]}
   :move-right {:label "Move Right"
                :keys  (set ["right" "l" "d"])
                :event [:games.puyo.events/move-piece name :right]}
   :hold       {:label "Hold"
                :keys  (set ["space"])
                :event [:games.puyo.events/hold-and-swap-piece name]}
   :rotate     {:label "Rotate Piece"
                :keys  (set ["up" "k" "w"])
                :event [:games.puyo.events/rotate-piece name]}
   :pause      {:label "Pause"
                :keys  (set ["enter"])
                :event [:games.puyo.events/toggle-pause game-opts]}
   :controls   {:label "Controls"
                :keys  (set ["c"])
                :event [:games.puyo.events/set-view game-opts :controls]}
   :about      {:label "About"
                :keys  (set ["b"])
                :event [:games.puyo.events/set-view game-opts :about]}
   :game       {:label "Return to game"
                :keys  (set ["g"])
                :event [:games.puyo.events/set-view game-opts :game]}
   ;; TODO does not apply to only-puyo build
   :exit       {:label "Exit to main menu"
                :keys  (set ["x"])
                :event [:games.events/deselect-game]}})

(def piece-grid (grid/build-grid {:height     2
                                  :width      1
                                  :entry-cell {:x 0 :y 1}}))

(defn initial-db
  "Creates an initial puyo game state."
  ([] (initial-db {:name :default}))

  ([{:keys [name grid] :as game-opts}]
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
        grid))

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
    :preview-grids  (repeat 3 piece-grid)

    ;; controls
    :controls (initial-controls game-opts)

    ;; timer
    :time      0
    :timer-inc 100

    ;; hold/swap
    :falling-shape-fn nil
    :held-shape-fn    nil
    :held-grid        piece-grid
    :hold-lock        false

    ;; modes
    :spin-the-bottle? false
    :spin             false

    ;; level/score
    :level                 1
    :groups-per-level      5
    :groups-cleared        0
    :score                 0
    :score-per-group-clear 10
    :groups-in-combo       0
    :last-combo-piece-num  nil}))


