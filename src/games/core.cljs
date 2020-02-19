(ns games.core
  (:require
   [reagent.core :as reagent]
   [games.views :as views]
   [games.events :as events]
   [games.events.timeout] ;; make sure this is required
   [games.controls.events :as controls.events]
   [re-frame.core :as rf]))

(defn dev-setup []
  (enable-console-print!))

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

  (rf/dispatch-sync [::events/init]))
