(ns games.core
  (:require
   [reagent.core :as reagent]
   [games.views :as views]
   [games.events :as events]
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

  ;; set a listener for keydown events
  (rf/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])

  ;; initialize db
  (rf/dispatch-sync [::events/init-db])

  ;; start tetris
  ;; (rf/dispatch-sync [::tetris.events/start-game])

  ;; start puyo
  (rf/dispatch-sync [::puyo.events/start-game]))


(comment
  (rf/dispatch-sync [::tetris.events/pause-game])
  (rf/dispatch-sync [::tetris.events/game-tick]))
