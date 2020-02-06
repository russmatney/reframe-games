(ns games.core
  (:require
   [reagent.core :as reagent]
   [games.views :as views]
   [games.events :as events]
   [games.controls.events :as controls.events]
   [games.tetris.events :as tetris.events]
   [games.puyo.events :as puyo.events]
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

  ;; initialize db
  (rf/dispatch-sync [::events/init-db])

  ;; setup controls
  (rf/dispatch-sync [::controls.events/init])

  ;; TODO break the builds apart
  ;; start tetris
  ;; (rf/dispatch-sync [::tetris.events/start-game])

  ;; start puyo
  (rf/dispatch-sync [::puyo.events/start-game])
  )


(comment
  (rf/dispatch-sync [::tetris.events/pause-game])
  (rf/dispatch-sync [::tetris.events/game-tick]))
