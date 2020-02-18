(ns games.tetris.views.classic
  (:require
   [re-frame.core :as rf]
   [games.views.components :as components]
   [games.views.util :as util]
   [games.grid.views :as grid.views]
   [games.controls.views :as controls.views]
   [games.subs :as subs]
   [games.tetris.subs :as tetris.subs]
   [games.tetris.events :as tetris.events]
   [games.tetris.views :as tetris.views]
   [games.color :as color]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Center Panel
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn gameover
  []
  [:h3 {:style {:margin-bottom "1rem"}} "Game Over."])

(defn restart
  [game-opts]
  [:p
   {:style    {:margin-top "1rem"}
    ;; TODO impl event
    :on-click #(rf/dispatch [::tetris.events/restart-game game-opts])}
   "Click here to restart."])

(defn center-panel [game-opts]
  (let [grid      @(rf/subscribe [::tetris.subs/game-grid game-opts])
        gameover? @(rf/subscribe [::tetris.subs/gameover? game-opts])]
    [:div.center-panel
     {:style
      {:display "flex"
       :flex    "1"}}
     [components/widget
      {:style {:flex "1"}}

      (when gameover? ^{:key "go"} [gameover])
      ^{:key "matrix"} [tetris.views/matrix grid game-opts]
      (when gameover? ^{:key "rest."} [restart game-opts])]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Left panel
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
     [components/widget
      {:style
       {:flex "1"}
       :label (if paused? "Paused" "Time")
       :value time}]
     [components/widget
      {:style
       {:flex "1"}
       :label "Level" :value level}]
     [components/widget
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
       :cell->style (fn [c] {:background (color/cell->piece-color c)})
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
       :cell->style (fn [c] {:background (color/cell->piece-color c)})})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Right panel
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-game [game-opts]
  (let [fields [:falling-shape :piece-queue]
        db     @(rf/subscribe [::tetris.subs/tetris-db game-opts])]
    [components/widget
     {}
     [:div
      [:h3 "Game DB"]
      (for [f fields]
        [:p
         (str f ":=> " (get db f))])]]))

(defn right-panel [game-opts]
  [:div
   {:style
    {:display        "flex"
     :flex           "1"
     :flex-direction "column"}}
   (when false [debug-game game-opts])
   [piece-queue game-opts]
   [held-piece game-opts]
   [controls.views/mini-text
    {:controls [:pause :hold :rotate]}]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Classic game
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn classic-game
  []
  (let [game-opts {:name :tetris-classic-game}
        game-opts @(rf/subscribe [::tetris.subs/game-opts game-opts])]
    [components/page
     {:direction    :row
      :full-height? true}
     ^{:key "left"}
     [left-panel game-opts]

     ^{:key "center"}
     [center-panel game-opts]

     ^{:key "right"}
     [right-panel game-opts]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main page component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page
  []
  [classic-game])
