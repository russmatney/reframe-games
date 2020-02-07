(ns games.core
  (:require
   [reagent.core :as reagent]
   [games.views :as views]
   [games.events :as events]
   [games.events.timeout] ;; make sure this is required
   [games.controls.events :as controls.events]
   [re-frame.core :as rf]))

(goog-define GAME false)

(defn dev-setup []
  (enable-console-print!)
  (println "dev mode"))

(defn mount-root []
  (reagent/render [views/root]
                  (.getElementById js/document "app")))

(defn ^:export init
  "Called on page-load in public/index.html.
  Only called once - does not get called on 'live-reloads' during development.
  "
  []
  (dev-setup)
  (mount-root)

  ;; initialize db
  (rf/dispatch-sync [::events/init-db])

  ;; setup controls
  (rf/dispatch-sync [::controls.events/init])

  (let [game (case GAME
               "tetris" :tetris
               "puyo"   :puyo
               nil      nil)]
    (rf/dispatch-sync [::events/select-game game])))
