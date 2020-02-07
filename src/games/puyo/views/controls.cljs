(ns games.puyo.views.controls
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [games.puyo.subs :as puyo.subs]
   [games.views.components :refer [widget]]))

(defn display-control [[control {:keys [keys event label]}]]
  ^{:key control}
  [widget
   {:on-click #(rf/dispatch event)
    :style
    {:flex "1 0 25%"}
    :label    label
    :children [^{:key (str keys)}
               [:p (string/join "," keys)]]}])

(defn view []
  (let [controls @(rf/subscribe [::puyo.subs/controls])]
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
