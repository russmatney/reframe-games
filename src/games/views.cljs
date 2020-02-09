(ns games.views
  (:require
   [games.tetris.views :as tetris.views]
   [games.puyo.views :as puyo.views]
   [games.controls.views :as controls.views]
   [games.views.about :as views.about]
   [games.subs :as subs]
   [games.select.views :as select.views]
   [re-frame.core :as rf]))

(defn root []
  (let [view @(rf/subscribe [::subs/current-view])]
    [:div#root
     {:style {:width "100vw"}}
     ;; TODO update view to 'page'?
     (case view
       :controls [controls.views/page]
       :about    [views.about/page]

       ;; games
       :tetris [tetris.views/page]
       :puyo   [puyo.views/page]

       nil [select.views/page])]))


