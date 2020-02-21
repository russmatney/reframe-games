(ns games.views
  (:require
   [games.puyo.views.classic :as puyo.classic]
   [games.tetris.views.classic :as tetris.classic]
   [games.debug.views :as debug.views]
   [games.views.about :as views.about]
   [games.subs :as subs]
   [games.select.views :as select.views]
   [re-frame.core :as rf]))

(defn show-page
  []
  (let [page @(rf/subscribe [::subs/current-page])
        page (or page :select)]
    (case page
      ;; TODO restore proper controls view, perhaps as a modal intead of a page
      ;; :controls [controls.views/page]
      :controls [debug.views/page]
      :about    [views.about/page]

      :tetris [tetris.classic/page]
      :puyo   [puyo.classic/page]

      :debug  [debug.views/page]
      :select [select.views/page])))

(defn root []
  (rf/clear-subscription-cache!)
  [:div#root
   {:style {:width "100vw"}}
   [show-page]])

