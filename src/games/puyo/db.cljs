(ns games.puyo.db
  (:require
   [games.grid.core :as grid]
   [games.puyo.shapes :as puyo.shapes]
   [games.puyo.controls :as puyo.controls]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game DB
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def shape-grid
  (grid/build-grid
    {:height     2
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
  [game-opts]
  (let [{:keys
         [name game-grid step-timeout ignore-controls
          group-size
          ] :as game-opts}
        (merge defaults game-opts)]
    {:name            name
     :game-opts       game-opts
     :init-event-name :games.puyo.events/init-game
     :stop-event-name :games.puyo.events/stop

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
     :group-size        (or group-size 4) ;; number of puyos in a group to be removed
     :step-timeout      step-timeout
     :paused?           false
     :gameover?         false
     :waiting-for-fall? false
     :current-piece-num 0

     ;; timer
     :time 0

     ;; queue
     :piece-queue    (puyo.shapes/next-bag {:game-opts      game-opts
                                            :min-queue-size 5})
     :min-queue-size 5
     :preview-grids  (repeat 3 shape-grid)

     ;; controls
     :controls        (puyo.controls/initial game-opts)
     :ignore-controls ignore-controls

     ;; hold/swap
     :falling-shape nil
     :held-shape    nil
     :held-grid     shape-grid
     :hold-lock     false

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
     :last-combo-piece-num  nil}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game DBs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def classic-game-db
  (->
    {:name        :puyo-classic-game
     :cell-style  {:width "20px" :height "20px"}
     :pages       #{:puyo}
     :no-walls-x? true
     :game-grid   {:entry-cell {:x 3 :y -1}
                   :height     16
                   :width      8}}
    (game-db)))

(def select-game-db
  (-> {:name         :puyo-select-game
       :pages        #{:select}
       :tick-timeout 500
       :on-gameover  :restart
       :no-walls-x?  true
       :game-grid    {:entry-cell {:x 1 :y 0}
                      :height     10
                      :width      5}}
      (game-db)))

(def debug-game-db
  (->
    {:name        :puyo-debug-game
     :pages       #{:debug}
     :on-gameover :restart
     :colors      [:red :blue]
     :group-size  3
     :game-grid   {:entry-cell {:x 1 :y 0}
                   :height     5
                   :width      3}}
    (game-db)))

(def game-dbs
  [select-game-db
   classic-game-db
   debug-game-db])

;; TODO dry up
(def game-dbs-map
  (->> game-dbs
       (map (fn [game] [(-> game :game-opts :name) game]))
       (into {})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DB
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def db
  {:games game-dbs-map})
