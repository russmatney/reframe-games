(ns games.views
  (:require
   [games.tetris.views :as tetris.views]
   [games.puyo.views :as puyo.views]
   [games.puyo.views.classic :as puyo.classic]
   [games.controls.views :as controls.views]
   [games.views.about :as views.about]
   [games.subs :as subs]
   [games.select.views :as select.views]
   [re-frame.core :as rf]))

(defn show-page
  []
  (let [page @(rf/subscribe [::subs/current-page])
        page (or page :select)]
    (case page
      :controls [controls.views/page]
      :about    [views.about/page]

      :tetris [tetris.views/page]
      :puyo   [puyo.classic/page]

      :debug  [puyo.views/debug-game]
      :select [select.views/page])))

(defn root []
  (rf/clear-subscription-cache!)
  [:div#root
   {:style {:width "100vw"}}
   [show-page]])

