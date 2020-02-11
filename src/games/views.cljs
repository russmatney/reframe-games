(ns games.views
  (:require
   [games.tetris.views :as tetris.views]
   [games.puyo.views :as puyo.views]
   [games.controls.views :as controls.views]
   [games.views.about :as views.about]
   [games.subs :as subs]
   [games.select.views :as select.views]
   [re-frame.core :as rf]))

(defn show-page
  [page]
  (let [default-page @(rf/subscribe [::subs/default-page])
        page         (or page default-page :select)]
    (case page
      :controls [controls.views/page]
      :about    [views.about/page]

      :tetris [tetris.views/page]
      :puyo   [puyo.views/page]

      :select [select.views/page])))

(defn root []
  (let [page @(rf/subscribe [::subs/current-page])]
    [:div#root
     {:style {:width "100vw"}}
     [show-page page]]))

