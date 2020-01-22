(ns toying.core
  (:require
   [reagent.core :as reagent]
   [toying.views :as views]
   [toying.events :as events]
   [re-frame.core :as rf]
   [re-pressed.core :as rp]))

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

  ;; set a listener for keydown events
  (rf/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])

  ;; initialize db
  (rf/dispatch-sync [::events/init-db])

  ;; start the game
  (rf/dispatch-sync [::events/game-tick]))


(comment
  (rf/dispatch-sync [::events/pause-game])
  (rf/dispatch-sync [::events/game-tick]))
