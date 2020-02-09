(ns games.puyo.views
  (:require
   [re-frame.core :as rf]
   [games.views.components :refer [widget]]
   [games.views.util :as util]
   [games.controls.db :as controls.db]
   [games.grid.core :as grid]
   [games.grid.views :as grid.views]
   [games.puyo.events :as puyo.events]
   [games.puyo.subs :as puyo.subs]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def board-black "#212529")

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
  ;; [{:keys [x y]}]
  [_]
  board-black
  ;; (str "rgba(" (* x 20) ", 100, " (* x 20) ", " (- 1 (/ y 10)) ")")
  )

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

(defn gameover
  []
  ^{:key "go"}
  [:h3 {:style {:margin-bottom "1rem"}} "Game Over."])

(defn restart
  [game-opts]
  ^{:key "rest."}
  [:p
   {:style    {:margin-top "1rem"}
    :on-click #(rf/dispatch [::puyo.events/start-game game-opts])}
   "Click here to restart."])

(defn center-panel [{:keys [name] :as game-opts}]
  (let [gameover? @(rf/subscribe [::puyo.subs/gameover? name])]
    [:div.center-panel
     {:style
      {:display "flex"
       :flex    "1"}}
     [widget
      {:style {:flex "1"}}
      (when gameover? [gameover])

      ^{:key "matrix"} [matrix]

      (when gameover? [restart game-opts])]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Left panel
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn left-panel [{:keys [name]}]
  (let [score   @(rf/subscribe [::puyo.subs/score name])
        t       @(rf/subscribe [::puyo.subs/time name])
        level   @(rf/subscribe [::puyo.subs/level name])
        paused? @(rf/subscribe [::puyo.subs/paused? name])
        time    (str (util/with-precision 1 (/ t 1000)) "s")]
    [:div.left-panel
     {:style
      {:display        "flex"
       :flex           "1"
       :flex-direction "column"}}
     [widget
      {:on-click #(rf/dispatch [::puyo.events/toggle-pause name])
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

(defn piece-queue [{:keys [name]}]
  (let [preview-grids @(rf/subscribe [::puyo.subs/preview-grids name])]
    (grid.views/piece-list
      {:label       "Queue"
       :piece-grids preview-grids
       :cell->style (fn [c] {:background (cell->piece-color c)})})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Held Piece
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn hold-string [name]
  (let [any-held? @(rf/subscribe [::puyo.subs/any-held? name])
        hold-keys @(rf/subscribe [::puyo.subs/keys-for name :hold])
        hold-key  (first hold-keys)]
    (str (if any-held? "Swap (" "Hold (") hold-key ")")))

(defn held-piece [{:keys [name]}]
  (let [held-grid @(rf/subscribe [::puyo.subs/held-grid name])]
    (grid.views/piece-list
      {:label       (hold-string name)
       :piece-grids [held-grid]
       :cell->style
       (fn [{:keys [color] :as c}]
         (if color
           {:background (cell->piece-color c)}
           {:background "transparent"}))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls-mini
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn controls-mini [{:keys [name]}]
  [widget
   {:style
    {:padding "0.9rem"
     :flex    "1"}}
   (doall
     (for [ctr [:pause :controls :about :rotate]]
       (let [label (controls.db/control->label ctr)
             event @(rf/subscribe [::puyo.subs/event-for name ctr])
             keys  @(rf/subscribe [::puyo.subs/keys-for name ctr])]
         (when (and keys event)
           ^{:key label}
           [:p
            {:style    {:margin-bottom "0.3rem"}
             :on-click #(rf/dispatch event)}
            (str label " (" (first keys) ")")]))))])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Right panel
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn right-panel [game-opts]
  [:div
   {:style
    {:display        "flex"
     :flex           "1"
     :flex-direction "column"}}
   [piece-queue game-opts]
   [held-piece game-opts]
   [controls-mini game-opts]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Page game wrapper
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn game-view [game-opts]
  [:div
   {:style
    {:height  "100%"
     :width   "100%"
     :display "flex"}}
   [left-panel game-opts]
   [center-panel game-opts]
   [right-panel game-opts]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Mini-game
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def mini-game-defaults
  {:name      :default
   :game-grid {:entry-cell {:x 1 :y 0}
               :height     8
               :width      4}})

(defn mini-game
  "Intended as a div.
  Starts itself.

  Establishes sane defaults for a mini-player."
  ([] (mini-game {}))
  ([game-opts]
   (let [game-opts (merge mini-game-defaults game-opts)]
     (rf/dispatch [::puyo.events/start-game game-opts])
     [:div
      [matrix game-opts]])))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Full Page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def background-color "#441086")
(def background
  (str "linear-gradient(135deg, " background-color " 21px,
       black 22px, black 24px, transparent 24px, transparent 67px,
       black 67px, black 69px, transparent 69px),
       linear-gradient(225deg, " background-color " 21px,
       black 22px, black 24px, transparent 24px, transparent 67px,
       black 67px, black 69px, transparent 69px), 64px"))

(def page-game-defaults
  {:name      :default
   :game-grid {:entry-cell {:x 3 :y -1}
               :height     12
               :width      8}})

(defn page
  ([] (page {}))
  ([game-opts]
   (let [game-opts (merge page-game-defaults game-opts)]
     (rf/dispatch [::puyo.events/start-game game-opts])
     [:div
      {:style
       {:height           "100vh"
        :width            "100vw"
        :display          "flex"
        :background       background
        :background-color background-color
        :background-size  "64px 128px"
        :padding          "24px"}}
      [game-view game-opts]])))
