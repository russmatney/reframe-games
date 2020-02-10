(ns games.tetris.views
  (:require
   [re-frame.core :as rf]
   [games.views.components :refer [widget]]
   [games.views.util :as util]
   [games.grid.views :as grid.views]
   [games.controls.views :as controls.views]
   [games.subs :as subs]
   [games.tetris.subs :as tetris.subs]
   [games.tetris.events :as tetris.events]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Grid
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def board-black "#212529")

(defn matrix
  "Returns the rows of cells."
  ([] [matrix {:name :default}])
  ([{:keys [cell-style] :as game-opts}]
   (let [grid @(rf/subscribe [::tetris.subs/game-grid game-opts])]
     (grid.views/matrix
       grid
       {:cell->style
        (fn [c]
          (merge
            (or cell-style {})
            (if (:style c)
              (:style c)
              {:background board-black})))}))))

(defn center-panel [game-opts]
  (let [gameover? @(rf/subscribe [::tetris.subs/gameover? game-opts])]
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
      [matrix game-opts]
      (when gameover?
        ^{:key "rest."}
        [:p
         {:style    {:margin-top "1rem"}
          :on-click #(rf/dispatch [::tetris.events/start-game game-opts])}
         "Click here to restart."])]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Left panel

(defn left-panel [game-opts]
  (let [score   @(rf/subscribe [::tetris.subs/score game-opts])
        t       @(rf/subscribe [::tetris.subs/time game-opts])
        level   @(rf/subscribe [::tetris.subs/level game-opts])
        paused? @(rf/subscribe [::tetris.subs/paused? game-opts])
        time    (str (util/with-precision 1 (/ t 1000)) "s")]
    [:div.left-panel
     {:style
      {:display        "flex"
       :flex           "1"
       :flex-direction "column"}}
     [widget
      {:style
       {:flex "1"}
       :label (if paused? "Paused" "Time")
       :value time}]
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

(defn piece-queue [game-opts]
  (let [preview-grids @(rf/subscribe [::tetris.subs/preview-grids game-opts])]
    (grid.views/piece-list
      {:label       "Queue"
       :cell->style
       (fn [c]
         (merge
           (or (:cell-style game-opts) {})
           (:style c)))
       :piece-grids preview-grids})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hold/Swap
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn hold-string [game-opts]
  (let [any-held? @(rf/subscribe [::tetris.subs/any-held? game-opts])
        hold-keys @(rf/subscribe [::subs/keys-for :hold])
        hold-key  (first hold-keys)]
    (str (if any-held? "Swap (" "Hold (") hold-key ")")))

(defn held-piece [game-opts]
  (let [held-grid @(rf/subscribe [::tetris.subs/held-grid game-opts])]
    (grid.views/piece-list
      {:label       (hold-string game-opts)
       :piece-grids [held-grid]
       :cell->style
       (fn [c]
         (merge
           (or (:cell-style game-opts) {})
           (:style c)))})))

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
   [controls.views/mini
    {:controls [:pause :hold :rotate]}]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Mini-player
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def mini-game-defaults
  {:name      :default
   :game-grid {:entry-cell {:x 1 :y 0}
               :height     8
               :width      5}})

(defn mini-game
  "Intended as a div.
  Starts itself.

  Establishes sane defaults for a mini-player."
  ([] (mini-game {}))
  ([game-opts]
   (let [game-opts (merge mini-game-defaults game-opts)]
     (rf/dispatch [::tetris.events/start-game game-opts])
     [:div
      [matrix game-opts]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main page component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def background-color "#441086")
;;:background "#5d08c7"

(def page-game-defaults
  {:name       :default
   :cell-style {:width "20px" :height "20px"}
   :game-grid  {:entry-cell {:x 4 :y -1}
                :height     16
                :width      10}})

(defn page
  "Intended for a full browser window
  Expects to be started itself."
  ([] (page {}))
  ([game-opts]
   (let [game-opts (merge page-game-defaults game-opts)]
     (rf/dispatch [::tetris.events/start-game game-opts])
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

      ^{:key "left"}
      [left-panel game-opts]

      ^{:key "center"}
      [center-panel game-opts]

      ^{:key "right"}
      [right-panel game-opts]])))
