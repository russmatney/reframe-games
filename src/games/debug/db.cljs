(ns games.debug.db
  (:require
   [games.debug.core :as debug]
   [games.grid.core :as grid]
   [games.debug.controls :as debug.controls]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game DB
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def game-opts-defaults
  {:no-walls? true
   :debug?    false})

(defn initial-game-db
  ([] (initial-game-db {}))
  ([game-opts]
   (let [{:keys [name game-grid] :as game-opts}
         (merge game-opts-defaults game-opts)]
     {:name            name
      :game-opts       game-opts
      :init-event-name :games.debug.events/init-game
      :stop-event-name :games.debug.events/stop-game

      :game-grid (grid/build-grid
                   (merge
                     ;; TODO entry-types like (top, middle, bottom, left, right)
                     {:entry-cell      {:x 0 :y 0}
                      :height          3
                      :width           3
                      :phantom-columns 2
                      :phantom-rows    2}
                     game-grid))
      :controls  (debug.controls/initial game-opts)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game DBs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def select-game-db
  (-> {:name  :debug-select-game
       :pages #{:select}}
      (initial-game-db)
      (debug/add-pieces)))

(defn make-debug-game-db
  [name opts]
  (-> (merge
        {:name  name
         :pages #{:debug}}
        opts)
      (initial-game-db)
      (debug/add-pieces)))

(def game-dbs
  [select-game-db
   ;; (make-debug-game-db
   ;;   :debug-debug-game
   ;;   {:debug? true})
   (make-debug-game-db
     :debug-debug-game-1
     {:debug-game? true
      :game-grid   {:height          3 :width        3
                    :phantom-columns 1 :phantom-rows 1}
      :debug?      false})
   ;; (make-debug-game-db
   ;;   :debug-debug-game-2
   ;;   {:game-grid   {:height 3 :width 3}
   ;;    :debug-game? true
   ;;    :debug?      false})
   ;; (make-debug-game-db
   ;;   :debug-debug-game-3
   ;;   {:game-grid   {:height 3 :width 3}
   ;;    :cell-height "24px"
   ;;    :cell-width  "24px"
   ;;    :debug-game? true
   ;;    :debug?      false})
   ])

(def game-dbs-map
  (->> game-dbs
       (map (fn [game] [(-> game :game-opts :name) game]))
       (into {})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DB
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def db
  {:games game-dbs-map})
