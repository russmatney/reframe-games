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
     :flex-wrap "wrap"
     :flex-direction "row"
     :width "100%"
     :padding "24px"}}
   [widget
    {:style
     {:width "100%"}
     :label "Controls"}]
   [widget
    {:style
     {:flex "1"}
     :label "Move Left: h"}]])

(comment
  (rf/dispatch [::tetris.events/set-view :controls])
  (rf/dispatch [::tetris.events/set-view :game]))

   ;; [:div
   ;;  {:style
   ;;   {:width "100%"}}
   ;;  "Controls"]
   ;; [:div
   ;;  {:style {:flex "1"}}
   ;;  "Move Left: h"]
   ;; [:div
   ;;  {:style {:flex "1"}}
   ;;  "Move Left: h"]))
