(ns games.select.views
  (:require
   [re-frame.core :as rf]
   [games.events :as events]
   [games.tetris.views :as tetris.views]
   [games.puyo.views :as puyo.views]
   [games.debug.views :as debug.views]
   [games.puzzle.views :as puzzle.views]
   [games.views.components :as components]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Data
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def selectable-games
  [{:label     "Tetris"
    :on-click  #(rf/dispatch [::events/set-page :tetris])
    :component [tetris.views/select-game]}
   {:label     "Puyo"
    :on-click  #(rf/dispatch [::events/set-page :puyo])
    :component [puyo.views/select-game]}
   {:label     "Puzzle"
    :on-click  #(rf/dispatch [::events/set-page :puzzlle])
    :component [puzzle.views/select-game]}
   {:label     "Debug"
    :on-click  #(rf/dispatch [::events/set-page :debug])
    :component [debug.views/select-game]}])

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
     (selection game))])


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
   ^{:key "selections-comp"}
   [selections]])
