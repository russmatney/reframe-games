(ns games.tetris.views
  (:require
   [re-frame.core :as rf]
   [games.views.components :refer [widget]]
   [games.views.util :as util]
   [games.grid.views :as grid.views]
   [games.controls.db :as controls.db]
   [games.tetris.subs :as tetris.subs]
   [games.tetris.events :as tetris.events]
   [games.tetris.views.controls :as controls]
   [games.tetris.views.about :as about]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Grid
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn matrix
  "Returns the rows of cells."
  ([] [matrix {:name :default}])
  ([{:keys [name] :as db}]
   (let [grid @(rf/subscribe [::tetris.subs/game-grid name])]
     (grid.views/matrix grid {:cell->style :style}))))

(defn center-panel []
  (let [gameover? @(rf/subscribe [::tetris.subs/gameover?])]
    [:div.center-panel
     {:style
      {:display "flex"
       :flex    "1"}}
     [widget
      {:style
       {:flex "1"}}
      (when gameover?
        ^{:key "go"}
        [:h3
         {:style {:margin-bottom "1rem"}}
         "Game Over."])
      ^{:key "matrix"}
      [matrix]
      (when gameover?
        ^{:key "rest."}
        [:p
         {:style    {:margin-top "1rem"}
          :on-click #(rf/dispatch [::tetris.events/start-game])}
         "Click here to restart."])]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Left panel

(defn left-panel []
  (let [score   @(rf/subscribe [::tetris.subs/score])
        t       @(rf/subscribe [::tetris.subs/time])
        level   @(rf/subscribe [::tetris.subs/level])
        paused? @(rf/subscribe [::tetris.subs/paused?])
        time    (str (util/with-precision 1 (/ t 1000)) "s")]
    [:div.left-panel
     {:style
      {:display        "flex"
       :flex           "1"
       :flex-direction "column"}}
     [widget
      {:on-click #(rf/dispatch [::tetris.events/toggle-pause])
       :style
       {:flex "1"}
       :label    (if paused? "Paused" "Time")
       :value    time}]
     [widget
      {:style
       {:flex "1"}
       :label "Level" :value level}]
     [widget
      {:style
       {:flex "1"}
       :label "Score" :value score}]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Queue
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn piece-queue []
  (let [preview-grids @(rf/subscribe [::tetris.subs/preview-grids])]
    (grid.views/piece-list {:label       "Queue"
                            :cell->style :style
                            :piece-grids preview-grids})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hold/Swap
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn hold-string []
  (let [any-held? @(rf/subscribe [::tetris.subs/any-held?])
        hold-keys @(rf/subscribe [::tetris.subs/keys-for :hold])
        hold-key  (first hold-keys)]
    (str (if any-held? "Swap (" "Hold (") hold-key ")")))

(defn held-piece []
  (let [held-grid @(rf/subscribe [::tetris.subs/held-grid])]
    (grid.views/piece-list
      {:label       (hold-string)
       :piece-grids [held-grid]
       :cell->style :style})))

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
             event @(rf/subscribe [::tetris.subs/event-for ctr])
             keys  @(rf/subscribe [::tetris.subs/keys-for ctr])]
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Mini-player
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def mini-game-defaults
  {:name :default
   :grid {:entry-cell {:x 1 :y 0}
          :height     8
          :width      5}})


(defn mini-game
  "Intended as a div.
  Starts itself.

  Establishes sane defaults for a mini-player."
  ([] (mini-game {}))
  ([game-opts]
   (let [opts (merge mini-game-defaults game-opts)]
     (rf/dispatch [::tetris.events/start-game opts])
     [:div
      [matrix opts]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main page component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def background-color "#441086")
;;:background "#5d08c7"

(defn page
  "Intended for a full browser window
  Expects to be started itself."
  ([]
   (page {:name :default}))
  ([{:keys [name] :as game-opts}]
   (let [current-view @(rf/subscribe [::tetris.subs/current-view name])
         comps
         (case current-view
           :controls
           [^{:key "controls"}
            [controls/view]]

           :about
           [^{:key "about"}
            [about/view]]

           :game
           [^{:key "left"}
            [left-panel]
            ^{:key "center"}
            [center-panel]
            ^{:key "right"}
            [right-panel]])]
     [:div
      {:style
       {:height           "100vh"
        :width            "100vw"
        :display          "flex"
        :background
        (str "linear-gradient(135deg, " background-color " 21px, black 22px, black 24px, transparent 24px, transparent 67px, black 67px, black 69px, transparent 69px),
       linear-gradient(225deg, " background-color " 21px, black 22px, black 24px, transparent 24px, transparent 67px, black 67px, black 69px, transparent 69px)0 64px")
        :background-color background-color
        :background-size  "64px 128px"
        :padding          "24px"}}
      (for [c comps] c)])))
