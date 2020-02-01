(ns games.tetris.views.about
  (:require
   [re-frame.core :as rf]
   [games.tetris.events :as tetris.events]
   [games.tetris.views.components :refer [widget display-label]]))


(defn view []
  [widget
   {:style
    {:width "100%"}
    :label "about"}])

(comment
  (rf/dispatch [::tetris.events/set-view :about])
  (rf/dispatch [::tetris.events/set-view :game]))
