(ns games.tetris.views.controls
  (:require
   [re-frame.core :as rf]
   [games.tetris.subs :as tetris.subs]
   [games.tetris.events :as tetris.events]
   [games.tetris.views.components :refer [widget display-label]]))

(defn view []
  [:div
   {:style
    {:display "flex"
     :padding "24px"}}
   [widget
    {:style {:flex "3"}
     :label "Controls"}]
   [widget
    {:label "Move Left"
     :value "h"}]])

(comment
  (rf/dispatch [::tetris.events/set-view :controls])
  (rf/dispatch [::tetris.events/set-view :game]))
