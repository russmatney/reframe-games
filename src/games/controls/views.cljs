(ns games.controls.views
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [games.subs :as subs]
   [games.views.components :as components]
   [games.grid.views :as grid.views]
   [games.controls.events :as controls.events]
   [games.controls.subs :as controls.subs]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Page control display
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Bring control display back
(defn display-control [[control {:keys [keys event label]}]]
  ^{:key control}
  [components/widget
   {:on-click #(rf/dispatch event)
    :style
    {:flex "1 0 25%"}
    :label    label}
   ^{:key (str keys)}
   [:p (string/join "," keys)]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls-mini
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mini-text
  "Lists passed controls in text"
  [{:keys [controls]}]
  [components/widget
   {:style
    {:padding "0.9rem"
     :flex    "1"}}
   (doall
     (for [ctr controls]
       (let [{:keys [label event keys]}
             @(rf/subscribe [::subs/controls-for ctr])]
         (when (and keys event)
           ^{:key label}
           [:p
            {:style    {:margin-bottom "0.3rem"}
             :on-click #(rf/dispatch event)}
            (str label " (" (first keys) ")")]))))])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Select Controls game
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn select-game-cells
  [{:keys [moveable? x y]}]
  ^{:key (str x y)}
  [:div
   {:style
    {:height     "48px"
     :width      "48px"
     :border     (if moveable? "1px solid white" "1px solid black")
     :background (if moveable? "green" "white")}}
   ""])

(defn select-game
  "Intended as a div. Starts itself."
  []
  (let [game-opts {:name :controls-select-game}
        grid      @(rf/subscribe [::controls.subs/game-grid game-opts])]
    [grid.views/matrix grid
     {:->cell select-game-cells}]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Debug game
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-cells
  "Debug cells have clickable `:anchor?`s."
  [{:keys [moveable? x y anchor?] :as cell}
   {:keys [debug? cell-height cell-width] :as game-opts}]
  (let [cell-width  (or cell-width (if debug? "148px" "48px"))
        cell-height (or cell-height (if debug? "148px" "48px"))]
    ^{:key (str x y)}
    [:div
     {:on-click
      (when anchor?
        #(rf/dispatch [::controls.events/toggle-debug game-opts]))
      :style
      {:height     cell-height
       :width      cell-width
       :border     (if moveable? "1px solid white" "1px solid red")
       :background (cond
                     anchor?   "blue"
                     moveable? "green"
                     :else     "white")}}
     (if debug? (str cell) "")]))

(defn debug-game
  "Intended as a full page.
  Useful as a debugger and sandbox, for implementing fancy features.
  Click the anchor cell to toggle `debug`.
  "
  ([] (debug-game {:name :controls-debug-game}))
  ([game-opts]
   (let [grid      @(rf/subscribe [::controls.subs/game-grid game-opts])
         debug?    @(rf/subscribe [::controls.subs/debug? game-opts])
         game-opts @(rf/subscribe [::controls.subs/game-opts game-opts])]

     [:div
      (when debug? [:h1 {:style {:color "white"}} (str "debug? :" debug?)])

      (when debug? [:h3 {:style {:color "white"}} (:name game-opts)])
      [grid.views/matrix grid {:->cell #(debug-cells % game-opts)}]

      (when debug? [:div {:style {:background "white"}} [:p (str game-opts)]])]
     )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls Pages
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn n-games-page []
  (let [
        debug-game-opts @(rf/subscribe [::controls.subs/debug-game-opts])
        ]
    [:div
     {:style {:width           "100%"
              :display         "flex"
              :justify-content "space-around"}}
     (for [opts debug-game-opts]
       ^{:key (:name opts)}
       [components/widget
        {:label (:name opts)}
        [debug-game opts]])]))

(defn page []
  [components/page
   {:direction    :row
    :full-height? true
    :header       [components/widget {:label "Controls"}]
    }
   ;; ^{:key "debug-game"}
   ;; [debug-game]
   ^{:key "two-games"}
   [n-games-page]
   ])
