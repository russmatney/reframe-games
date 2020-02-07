(ns games.tetris.views.controls
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [games.tetris.subs :as tetris.subs]
   [games.tetris.events :as tetris.events]
   [games.views.components :refer [widget]]))

(defn display-control [[control {:keys [keys event label]}]]
  ^{:key control}
  [widget
   {:on-click #(rf/dispatch event)
    :style
    {:flex "1 0 25%"}
    :label    label}
   ^{:key (str keys)}
   [:p (string/join "," keys)]])

(defn view []
  (let [controls @(rf/subscribe [::tetris.subs/controls])]
    [:div
     {:style
      {:display        "flex"
       :flex-wrap      "wrap"
       :flex-direction "row"
       :width          "100%"
       :padding        "24px"}}
     [widget
      {:style
       {:width "100%"}
       :label "Controls"}]
     (for [control controls]
       (display-control control))]))

(comment
  (rf/dispatch [::tetris.events/set-view :controls])
  (rf/dispatch [::tetris.events/set-view :game]))
