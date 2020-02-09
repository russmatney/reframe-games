(ns games.select.views
  (:require
   [re-frame.core :as rf]
   [games.events :as events]
   [games.tetris.views :as tetris.views]
   [games.puyo.views :as puyo.views]
   [games.views.components :refer [widget]]))

;; TODO display controls/about


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Data
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def selectable-games
  ;; TODO specify easier pieces, fewer colors
  [{:label     "Tetris"
    :on-click  #(rf/dispatch [::events/select-game :tetris])
    :component [tetris.views/mini-game
                {:game-grid       {:height 10 :width 5 :entry-cell {:x 2 :y -1}}
                 :tick-timeout    500
                 :ignore-controls true}]}
   {:label     "Puyo"
    :on-click  #(rf/dispatch [::events/select-game :puyo])
    :component [puyo.views/mini-game
                {:game-grid       {:height 10 :width 5 :entry-cell {:x 2 :y -1}}
                 :tick-timeout    500
                 :ignore-controls true}]}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Selections
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn selection
  [{:keys [label on-click component]}]
  [widget
   {:class    "selection"
    :on-click on-click
    :label    label
    :style    {:flex "1"}}
   [:div
    {:style
     {:flex "1"}}
    component]])

(defn selections
  []
  [:div
   {:style
    {:width   "100%"
     :flex    "1"
     :display "flex"}}
   (for [game selectable-games]
     (selection game))])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Expectations
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO support this copy well enough to include
(defn expectations
  "TODO fade-in effect, maybe on the widget api?"
  []
  [widget
   {
    :style   {:flex "1"}
    :label   "Expectations"
    :subhead [:ul {:style {:list-style "none"}}
              [:li "Move with mouse*, arrow keys, wasd, or vim bindings"]
              [:li "Enter, Click, or Score to choose."]
              [:li "*Mouse not yet implemented*"]]}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Header
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn header []
  [widget
   {:style {}
    :label "Select a game"}])

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
       :padding "24px"}
      )}
   [:div
    {:style {:display        "flex"
             :flex-direction "column"
             :height         "100%"
             :width          "100%"}}
    [header]
    ;; [expectations]
    [selections]]])
