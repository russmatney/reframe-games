(ns games.select.views
  (:require
   [re-frame.core :as rf]
   [games.events :as events]
   [games.subs :as subs]
   [games.views.components :refer [widget]]))

;; TODO display controls/about here


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Data
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def games
  [{:label     "Tetris"
    :on-click  #(rf/dispatch [::events/select-game :tetris])
    :component [:div
                [:p "tetris-game"]]}
   {:label     "Puyo"
    :on-click  #(rf/dispatch [::events/select-game :puyo])
    :component [:div
                [:p "puyo-game"]]}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Selections
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn selection
  [{:keys [label on-click component]}]
  [widget
   {:label label
    :style {:flex "1"}}
   [:div.select-view
    {:on-click on-click
     :style
     {:background "coral"
      :flex       "1"}}
    component]])

(defn selections []
  [:div
   {:style
    {:width   "100%"
     :display "flex"}}
   (for [game games]
     (selection game))])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Header
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn header []
  [widget {:label "Select a game"}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main page component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def background-color "#441086")
(def background-style
  {:background
   (str "linear-gradient(135deg, " background-color " 21px, black 22px, black 24px, transparent 24px, transparent 67px, black 67px, black 69px, transparent 69px),
       linear-gradient(225deg, " background-color " 21px, black 22px, black 24px, transparent 24px, transparent 67px, black 67px, black 69px, transparent 69px)0 64px")
   :background-size  "64px 128px"
   :background-color background-color})

(defn page []
  [:div
   {:style
    (conj
      background-style
      {:height  "100vh"
       :width   "100vw"
       :display "flex"
       :padding "24px"}
      )}
   [:div
    {:style {:height "100%" :width "100%"}}
    [header]
    [selections]]])
