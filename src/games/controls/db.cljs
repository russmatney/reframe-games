(ns games.controls.db
  (:require
   [games.grid.core :as grid]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Global controls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def global-controls
  {:home     {:label "Home"
              :keys  (set ["m" "x"])
              :event [:games.events/unset-page]}
   :controls {:label "Controls"
              :keys  (set ["c" "?"])
              :event [:games.events/set-page :controls]}
   :about    {:label "About"
              :keys  (set ["b"])
              :event [:games.events/set-page :about]}
   :tetris   {:label "Play Tetris"
              :keys  (set ["t"])
              :event [:games.events/set-page :tetris]}
   :puyo     {:label "Play Puyo-Puyo"
              :keys  (set ["p"])
              :event [:games.events/set-page :puyo]}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls Game DB
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def db-defaults
  {:game-grid {:entry-cell      {:x 0 :y 0}
               :height          3
               :width           3
               :phantom-columns 2
               :phantom-rows    2}})

(defn controls-game-controls
  "heh."
  [game-opts]
  {:add-piece  {:label "Add Piece"
                :keys  (set ["space"])
                :event [:games.controls.events/add-piece game-opts]}
   :move-left  {:label "Move Left"
                :keys  (set ["left" "h" "a"])
                :event [:games.controls.events/move-piece game-opts :left]}
   :move-down  {:label "Move Down"
                :keys  (set ["down" "j" "s"])
                :event [:games.controls.events/move-piece game-opts :down]}
   :move-right {:label "Move Right"
                :keys  (set ["right" "l" "d"])
                :event [:games.controls.events/move-piece game-opts :right]}
   :move-up    {:label "Move Up"
                :keys  (set ["up" "k" "s"])
                :event [:games.controls.events/move-piece game-opts :up]}
   :rotate     {:label "Rotate"
                :keys  (set [])
                :event [:games.controls.events/rotate-piece game-opts]}})

(defn initial-db
  ([] (initial-db {}))
  ([game-opts]
   (let [{:keys [name game-grid] :as game-opts}
         (merge db-defaults game-opts)]
     {:name      name
      :game-opts game-opts
      :game-grid (grid/build-grid game-grid)
      :controls  (controls-game-controls game-opts)})))
