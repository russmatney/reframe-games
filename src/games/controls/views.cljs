(ns games.controls.views
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [games.subs :as subs]
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

;; TODO dry this up
(def background-color "#441086")
(def background
  (str "linear-gradient(135deg, " background-color " 21px,
       black 22px, black 24px, transparent 24px, transparent 67px,
       black 67px, black 69px, transparent 69px),
       linear-gradient(225deg, " background-color " 21px,
       black 22px, black 24px, transparent 24px, transparent 67px,
       black 67px, black 69px, transparent 69px), 64px"))

;; TODO create page component
(defn page []
  (let [controls @(rf/subscribe [::subs/controls])]
    [:div
     {:style
      {:height           "100vh"
       :width            "100vw"
       :display          "flex"
       :background       background
       :background-color background-color
       :background-size  "64px 128px"
       :padding          "24px"}}
     [:div
      {:style
       {:display        "flex"
        :flex-wrap      "wrap"
        :flex-direction "row"}}
      [widget
       {:style {:width "100%"}
        :label "Controls"}]
      (for [control controls]
        (display-control control))]]))
