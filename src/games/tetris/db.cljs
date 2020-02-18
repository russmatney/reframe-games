(ns games.tetris.db
  (:require
   [games.tetris.shapes :as tetris.shapes]
   [games.tetris.controls :as tetris.controls]
   [games.grid.core :as grid]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game DB
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def shape-grid
  (grid/build-grid
    {:height     2
     :width      4
     :entry-cell {:x 1 :y 1}}))

(def defaults
  {:step-timeout    500
   :ignore-controls false})

(defn game-db
  "Creates an initial tetris game-state."
  [game-opts]
  (let [{:keys [name game-grid step-timeout ignore-controls] :as game-opts}
        (merge defaults game-opts)]
    {:name      name
     :game-opts game-opts

     ;; game matrix
     :game-grid
     (grid/build-grid
       (merge
         {:height       10
          :width        10
          :phantom-rows 2
          :entry-cell   {:x 5 :y -1}}
         game-grid))

     ;; game logic
     :step-timeout step-timeout
     :paused?      false
     :gameover?    false

     ;; queue
     :piece-queue    (shuffle tetris.shapes/allowed-shapes)
     :min-queue-size 5
     :allowed-shapes tetris.shapes/allowed-shapes
     :preview-grids  (repeat 3 shape-grid)

     ;; controls
     :controls        (tetris.controls/initial game-opts)
     :ignore-controls ignore-controls

     ;; hold/swap
     :falling-shape nil
     :held-shape    nil
     :held-grid     shape-grid
     :hold-lock     false

     ;; timer
     :time 0

     ;; level/score
     :level                1
     :rows-per-level       5
     :rows-cleared         0
     :pieces-played        0
     :score                0
     :score-per-row-clear  10
     :rows-in-combo        0
     :last-combo-piece-num nil
     }))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game DBs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def classic-game-db
  (->
    {:name       :tetris-classic-game
     :cell-style {:width "20px" :height "20px"}
     :pages      #{:tetris}
     :game-grid  {:entry-cell {:x 4 :y -1}
                  :height     16
                  :width      10}}
    (game-db)))

(def select-game-db
  (-> {:name         :tetris-select-game
       :pages        #{:select}
       :tick-timeout 500
       :on-gameover  :restart
       :no-walls-x?  true
       :game-grid    {:height 10 :width 5 :entry-cell {:x 2 :y -1}}}
      (game-db)))

(def game-dbs
  [select-game-db
   classic-game-db])

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
