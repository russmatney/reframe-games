(ns games.controls.db
  (:require
   [games.grid.core :as grid]
   [games.controls.core :as controls]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Global controls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def global-controls
  [{:id    :home
    :label "Home"
    :keys  (set ["m" "x"])
    :event [:games.events/unset-page]}
   {:id    :controls
    :label "Controls"
    :keys  (set ["c" "?"])
    :event [:games.events/set-page :controls]}
   {:id    :about
    :label "About"
    :keys  (set ["b"])
    :event [:games.events/set-page :about]}
   {:id    :tetris
    :label "Play Tetris"
    :keys  (set ["t"])
    :event [:games.events/set-page :tetris]}
   {:id    :puyo
    :label "Play Puyo-Puyo"
    :keys  (set ["p"])
    :event [:games.events/set-page :puyo]}
   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls Game DB
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn controls-game-controls
  "heh."
  [game-opts]
  [{:id    :move-left
    :label "Move Left"
    :keys  (set ["left" "h" "a"])
    :event [:games.controls.events/move-piece game-opts :left]}
   {:id    :move-down
    :label "Move Down"
    :keys  (set ["down" "j" "s"])
    :event [:games.controls.events/move-piece game-opts :down]}
   {:id    :move-right
    :label "Move Right"
    :keys  (set ["right" "l" "d"])
    :event [:games.controls.events/move-piece game-opts :right]}
   {:id    :move-up
    :label "Move Up"
    :keys  (set ["up" "k" "s"])
    :event [:games.controls.events/move-piece game-opts :up]}
   {:id    :rotate
    :label "Rotate"
    :keys  (set ["space"])
    :event [:games.controls.events/rotate-piece game-opts]}])

(def game-opts-defaults
  {:no-walls? true
   :debug?    false})

(defn initial-game-db
  ([] (initial-game-db {}))
  ([game-opts]
   (let [{:keys [name game-grid] :as game-opts}
         (merge game-opts-defaults game-opts)]
     {:name      name
      :game-opts game-opts
      :game-grid (grid/build-grid
                   (merge
                     {:entry-cell      {:x 0 :y 0}
                      :height          3
                      :width           3
                      :phantom-columns 2
                      :phantom-rows    2}
                     game-grid))
      :controls  (controls-game-controls game-opts)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls Game DBs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def mini-game-db
  (-> {:name  :controls-mini-game
       :pages #{:select}}
      (initial-game-db)
      (controls/add-piece)))

(def page-game-db
  (-> {:name  :controls-page-game
       :pages #{:controls}}
      (initial-game-db)
      (controls/add-piece)))

(defn make-debug-game-db
  [name opts]
  (-> (merge
        {:name  name
         :pages #{:controls}}
        opts)
      (initial-game-db)
      (controls/add-piece)))

(def debug-game-db
  (make-debug-game-db :controls-debug-game {:debug? true}))
(def debug-game-db-1
  (make-debug-game-db :controls-debug-game-1 {:debug? false}))
(def debug-game-db-2
  (make-debug-game-db
    :controls-debug-game-2
    {:game-grid {:height 5
                 :width  5}
     :debug?    false}))

(def default-db
  (-> {:name :default}
      (initial-game-db)))

(def game-dbs
  [mini-game-db
   page-game-db
   debug-game-db
   debug-game-db-1
   debug-game-db-2
   default-db])

(def game-dbs-map
  (->> game-dbs
       (map (fn [game] [(-> game :game-opts :name) game]))
       (into {})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls DB
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def db
  {:controls       global-controls
   :controls-games game-dbs-map})
