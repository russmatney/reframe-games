(ns games.select.views
  (:require
   [re-frame.core :as rf]
   [games.events :as events]
   [games.tetris.views :as tetris.views]
   [games.puyo.views :as puyo.views]
   [games.controls.views :as controls.views]
   [games.views.components :as components]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Data
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def selectable-games
  ;; TODO specify easier pieces, fewer colors
  [{:label     "Tetris"
    :on-click  #(rf/dispatch [::events/set-view :tetris])
    :component [tetris.views/mini-game
                {:game-grid       {:height 10 :width 5 :entry-cell {:x 2 :y -1}}
                 :tick-timeout    500
                 :on-gameover     :restart
                 :no-walls-x?     true
                 :ignore-controls true}]}

   {:label     "Puyo"
    :on-click  #(rf/dispatch [::events/set-view :puyo])
    :component [puyo.views/mini-game
                {:game-grid       {:height 10 :width 5 :entry-cell {:x 2 :y -1}}
                 :tick-timeout    500
                 :on-gameover     :restart
                 :no-walls-x?     true
                 :ignore-controls true}]}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Selections
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn selection
  [{:keys [label on-click component]}]
  ^{:key label}
  [components/widget
   {:class    "selection"
    :on-click on-click
    :label    label
    :style    {:flex "1"}}
   ^{:key (str "selection-container-" label)}
   [:div
    {:style
     {:flex "1"}}
    ^{:key label}
    component]])

(defn selections
  []
  [:div.selections
   {:style
    {:width   "100%"
     :flex    "1"
     :display "flex"}}
   (for [{:keys [label] :as game} selectable-games]
     ^{:key label}
     (selection game))
   ^{:key "controls-mini-game"}
   [controls.views/mini-game]])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Expectations
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO support this copy well enough to include
(defn expectations
  "TODO fade-in effect, maybe on the widget api?"
  []
  [components/widget
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
  [components/widget
   {:class "header"
    :style {}
    :label "Select a game"}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main page component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page []
  [components/page
   {:direction :row
    :header    [header]}
   ;; [expectations]
   [selections]])
