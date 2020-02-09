(ns games.puyo.views
  (:require
   [re-frame.core :as rf]
   [games.views.components :refer [widget]]
   [games.views.util :as util]
   [games.controls.db :as controls.db]
   [games.controls.views :as controls.views]
   [games.grid.core :as grid]
   [games.grid.views :as grid.views]
   [games.puyo.events :as puyo.events]
   [games.puyo.subs :as puyo.subs]
   [games.puyo.views.about :as about]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def green "#92CC41")
(def red "#FE493C") ;;"#B564D4"
(def blue "#209CEE") ;;#6ebff5
(def yellow "#F7D51D")

(def color->piece-color
  {:green  green
   :red    red
   :blue   blue
   :yellow yellow})

(defn cell->piece-color
  [c]
  (-> c :color (color->piece-color)))

(defn cell->background
  [{:keys [x y]}]
  (str "rgba(" (* x 20) ", 100, " (* x 20) ", " (- 1 (/ y 10)) ")"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Grid
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn matrix
  ([] [matrix {:name :default}])
  ([{:keys [name]}]
   (let [grid          @(rf/subscribe [::puyo.subs/game-grid name])
         spin?         @(rf/subscribe [::puyo.subs/puyo-db name :spin-the-bottle?])
         pieces-played @(rf/subscribe [::puyo.subs/puyo-db name :pieces-played])

         grid
         (cond-> grid
           spin?
           (grid/spin {:reverse-y? (contains? #{1 2 3} (mod pieces-played 6))
                       :reverse-x? (contains? #{2 3 4} (mod pieces-played 6))}))]
     [grid.views/matrix grid {:cell->style
                              (fn [{:keys [color] :as c}]
                                (if color
                                  {:background (cell->piece-color c)}
                                  {:background (cell->background c)}))}])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Center Panel
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn center-panel []
  (let [gameover? @(rf/subscribe [::puyo.subs/gameover?])]
    [:div.center-panel
     {:style
      {:display "flex"
       :flex    "1"}}
     [widget
      {:style {:flex "1"}}
      (when gameover?
        ^{:key "go"}
        [:h3 {:style {:margin-bottom "1rem"}} "Game Over."])
      ^{:key "matrix"}
      [matrix]
      (when gameover?
        ^{:key "rest."}
        [:p
         {:style    {:margin-top "1rem"}
          :on-click #(rf/dispatch [::puyo.events/start-game])}
         "Click here to restart."])
      ]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Left panel
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn left-panel []
  (let [score   @(rf/subscribe [::puyo.subs/score])
        t       @(rf/subscribe [::puyo.subs/time])
        level   @(rf/subscribe [::puyo.subs/level])
        paused? @(rf/subscribe [::puyo.subs/paused?])
        time    (str (util/with-precision 1 (/ t 1000)) "s")]
    [:div.left-panel
     {:style
      {:display        "flex"
       :flex           "1"
       :flex-direction "column"}}
     [widget
      {:on-click #(rf/dispatch [::puyo.events/toggle-pause])
       :style    {:flex "1"}
       :label    (if paused? "Paused" "Time")
       :value    time}]
     [widget
      {:style {:flex "1"}
       :label "Level"
       :value level}]
     [widget
      {:style {:flex "1"}
       :label "Score"
       :value score}]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Piece Queue
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn piece-queue []
  (let [preview-grids @(rf/subscribe [::puyo.subs/preview-grids])]
    (grid.views/piece-list
      {:label       "Queue"
       :piece-grids preview-grids
       :cell->style (fn [c] {:background (cell->piece-color c)})})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Held Piece
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn hold-string []
  (let [any-held? @(rf/subscribe [::puyo.subs/any-held?])
        hold-keys @(rf/subscribe [::puyo.subs/keys-for :hold])
        hold-key  (first hold-keys)]
    (str (if any-held? "Swap (" "Hold (") hold-key ")")))

(defn held-piece []
  (let [held-grid @(rf/subscribe [::puyo.subs/held-grid])]
    (grid.views/piece-list
      {:label       (hold-string)
       :piece-grids [held-grid]
       :cell->style
       (fn [{:keys [color] :as c}]
         (if color
           {:background (cell->piece-color c)}
           {:background "transparent"}))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls-mini
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn controls-mini []
  [widget
   {:style
    {:padding "0.9rem"
     :flex    "1"}}
   (doall
     (for [ctr [:pause :controls :about :rotate]]
       (let [label (controls.db/control->label ctr)
             event @(rf/subscribe [::puyo.subs/event-for ctr])
             keys  @(rf/subscribe [::puyo.subs/keys-for ctr])]
         (when (and keys event)
           ^{:key label}
           [:p
            {:style    {:margin-bottom "0.3rem"}
             :on-click #(rf/dispatch event)}
            (str label " (" (first keys) ")")]))))])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Right panel
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn right-panel []
  [:div
   {:style
    {:display        "flex"
     :flex           "1"
     :flex-direction "column"}}
   [piece-queue]
   [held-piece]
   [controls-mini]])

(defn game-view []
  [:div
   {:style
    {:height  "100%"
     :width   "100%"
     :display "flex"}}
   [left-panel]
   [center-panel]
   [right-panel]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Mini-game
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def mini-game-defaults
  {:name :default
   :grid {:entry-cell {:x 1 :y 0}
          :height     8
          :width      4}})

(defn mini-game
  "Intended as a div.
  Starts itself.

  Establishes sane defaults for a mini-player."
  ([] (mini-game {}))
  ([game-opts]
   (let [opts (merge mini-game-defaults game-opts)]
     (rf/dispatch [::puyo.events/start-game opts])
     [:div
      [matrix opts]])))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Full Page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def background-color "#441086")
(def background-style
  (str "linear-gradient(135deg, " background-color " 21px,
       black 22px, black 24px, transparent 24px, transparent 67px,
       black 67px, black 69px, transparent 69px),
       linear-gradient(225deg, " background-color " 21px,
       black 22px, black 24px, transparent 24px, transparent 67px,
       black 67px, black 69px, transparent 69px), 64px"))

(defn page []
  (let [controls     @(rf/subscribe [::puyo.subs/controls])
        current-view @(rf/subscribe [::puyo.subs/current-view])]
    [:div
     {:style
      {:height           "100vh"
       :width            "100vw"
       :display          "flex"
       :background       background-style
       :background-color background-color
       :background-size  "64px 128px"
       :padding          "24px"}}
     (case current-view
       :controls [controls.views/view controls]
       :about    [about/view]
       :game     [game-view])]))
