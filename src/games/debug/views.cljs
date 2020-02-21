(ns games.debug.views
  (:require
   [re-frame.core :as rf]
   [games.views.components :as components]
   [games.grid.views :as grid.views]
   [games.debug.events :as debug.events]
   [games.debug.subs :as debug.subs]
   [games.subs :as subs]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Select game
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
  (let [game-opts {:name :debug-select-game}
        grid      @(rf/subscribe [::subs/game-grid game-opts])]
    [grid.views/matrix grid
     {:->cell select-game-cells}]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Debug game
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-cell
  "Debug cells have clickable `:anchor?`s."
  [{:keys [moveable? x y anchor?] :as cell}
   {:keys [debug? cell-height cell-width] :as game-opts}]
  (let [
        ;; cell-width  (or cell-width (if debug? "148px" "48px"))
        ;; cell-height (or cell-height (if debug? "148px" "48px"))
        cell-width  (or cell-width (if debug? "unset" "48px"))
        cell-height (or cell-height (if debug? "unset" "48px"))
        props       (dissoc cell :x :y)]
    ^{:key (str x y)}
    [:div
     {:on-click
      (when anchor?
        #(rf/dispatch [::debug.events/toggle-debug game-opts]))
      :style
      {:height     cell-height
       :width      cell-width
       :border     (if moveable? "1px solid white" "1px solid red")
       :background (cond
                     anchor?   "blue"
                     moveable? "green"
                     :else     "gray")}}
     (if debug? (str "x:" x " y:" y " " (when (seq props) props)) "")]))

(defn debug-game
  "Intended as a full page.
  Useful as a debugger and sandbox, for implementing fancy features.
  Click the anchor cell to toggle `debug`.
  "
  ([] (debug-game {:name :debug-debug-game}))
  ([game-opts]
   (let [grid      @(rf/subscribe [::subs/game-grid game-opts])
         debug?    @(rf/subscribe [::subs/game-db game-opts :debug?])
         game-opts @(rf/subscribe [::subs/game-opts game-opts])]

     [:div
      (when debug? [:h1 {:style {:color "white"}} (str "debug? :" debug?)])

      (when debug? [:h3 {:style {:color "white"}} (:name game-opts)])
      [grid.views/matrix grid {:->cell #(debug-cell % game-opts)}]

      (when debug? [:div {:style {:background "white"}} [:p (str game-opts)]])])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pages
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn n-games-page [n]
  (let [debug-game-opts @(rf/subscribe [::debug.subs/debug-game-opts])
        debug-game-opts (take n debug-game-opts)]
    [:div
     {:style {:width           "100%"
              :display         "flex"
              :justify-content "space-around"}}
     (for [opts debug-game-opts]
       ^{:key (:name opts)}
       [components/widget
        {:label (:name opts)}
        ^{:key "dbg-game"}
        [debug-game opts]])]))

(defn page []
  [components/page
   {:direction    :row
    :full-height? true
    :header       [components/widget {:label "Debug"}]}
   ;; ^{:key "debug-game"}
   ;; [debug-game]
   ^{:key "two-games"}
   [n-games-page 1]
   ])
