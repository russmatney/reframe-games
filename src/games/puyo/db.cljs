(ns games.puyo.db
  (:require
   [games.grid.core :as grid]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Piece Types
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO move to puyo core?
(defn build-piece-fn [game-opts]
  (let [colors (:colors game-opts)
        colorA (rand-nth colors)
        colorB (rand-nth colors)]
    (fn [{x :x y :y}]
      [{:x x :y y :anchor? true :color colorA}
       {:x x :y (- y 1) :color colorB}])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initial Controls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn initial-controls
  [game-opts]
  [{:id    :move-left
    :label "Move Left"
    :keys  (set ["left" "h" "a"])
    :event [:games.puyo.events/move-piece game-opts :left]}
   {:id    :move-down
    :label "Move Down"
    :keys  (set ["down" "j" "s"])
    :event [:games.puyo.events/move-piece game-opts :down]}
   {:id    :move-right
    :label "Move Right"
    :keys  (set ["right" "l" "d"])
    :event [:games.puyo.events/move-piece game-opts :right]}
   {:id    :hold
    :label "Hold"
    :keys  (set ["space"])
    :event [:games.puyo.events/hold-and-swap-piece game-opts]}
   {:id    :rotate
    :label "Rotate"
    :keys  (set ["up" "k" "w"])
    :event [:games.puyo.events/rotate-piece game-opts]}
   {:id    :pause
    :label "Pause"
    :keys  (set ["enter"])
    :event [:games.puyo.events/toggle-pause game-opts]}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game DB
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def piece-grid (grid/build-grid {:height     2
                                  :width      1
                                  :entry-cell {:x 0 :y 1}}))

(def defaults
  {:step-timeout    500
   :ignore-controls false
   :colors
   [:red
    :green
    :yellow
    :blue]})

(defn game-db
  "Creates an initial puyo game state."
  ([] (game-db {:name :default}))

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
      :piece-queue    (repeat 5 build-piece-fn)
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game DBs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def page-game-db
  (->
    {:name        :puyo-page-game
     :cell-style  {:width "20px" :height "20px"}
     :pages       #{:puyo}
     :no-walls-x? true
     :game-grid   {:entry-cell {:x 3 :y -1}
                   :height     16
                   :width      8}}
    (game-db)))

(def mini-game-db
  (-> {:name         :puyo-mini-game
       :pages        #{:select}
       :tick-timeout 500
       :on-gameover  :restart
       :no-walls-x?  true
       :game-grid    {:entry-cell {:x 1 :y 0}
                      :height     10
                      :width      5}}
      (game-db)))

(def default-db
  (-> {:name :default}
      (game-db)))

(def game-dbs
  [default-db
   mini-game-db
   page-game-db])

;; TODO dry up
(def game-dbs-map
  (->> game-dbs
       (map (fn [game] [(-> game :game-opts :name) game]))
       (into {})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DB
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def db
  ;; TODO rename to ::game-dbs
  {::db game-dbs-map})
