(ns games.puyo.views
  (:require
   [re-frame.core :as rf]
   [games.views.components :as components]
   [games.views.util :as util]
   [games.controls.views :as controls.views]
   [games.grid.core :as grid]
   [games.grid.views :as grid.views]
   [games.puyo.events :as puyo.events]
   [games.subs :as subs]
   [games.color :as color]
   [games.puyo.subs :as puyo.subs]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cells
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cell->style [game-opts {:keys [color] :as c}]
  (merge
    (or (:cell-style game-opts) {})
    (if color
      {:background (color/cell->piece-color c)}
      {:background (color/cell->background c)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Grid
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn matrix
  [grid game-opts]
  (let [spin?         @(rf/subscribe [::puyo.subs/puyo-db game-opts :spin-the-bottle?])
        pieces-played @(rf/subscribe [::puyo.subs/puyo-db game-opts :pieces-played])

        grid
        (cond-> grid
          spin?
          (grid/spin {:reverse-y? (contains? #{1 2 3} (mod pieces-played 6))
                      :reverse-x? (contains? #{2 3 4} (mod pieces-played 6))}))]
    [grid.views/matrix
     grid
     {:cell->style (partial cell->style game-opts)}]))

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

(defn center-panel [game-opts]
  (let [grid      @(rf/subscribe [::puyo.subs/game-grid game-opts])
        gameover? @(rf/subscribe [::puyo.subs/gameover? game-opts])]
    [:div.center-panel
     {:style
      {:display "flex"
       :flex    "1"}}
     [components/widget
      {:style {:flex "1"}}

      (when gameover? [gameover])
      ^{:key "matrix"} [matrix grid game-opts]
      (when gameover? [restart game-opts])]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Left panel
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn left-panel [game-opts]
  (let [score   @(rf/subscribe [::puyo.subs/score game-opts])
        t       @(rf/subscribe [::puyo.subs/time game-opts])
        level   @(rf/subscribe [::puyo.subs/level game-opts])
        paused? @(rf/subscribe [::puyo.subs/paused? game-opts])
        time    (str (util/with-precision 1 (/ t 1000)) "s")]
    [:div.left-panel
     {:style
      {:display        "flex"
       :flex           "1"
       :flex-direction "column"}}
     [components/widget
      {:on-click #(rf/dispatch [::puyo.events/toggle-pause game-opts])
       :style    {:flex "1"}
       :label    (if paused? "Paused" "Time")
       :value    time}]
     [components/widget
      {:style {:flex "1"}
       :label "Level"
       :value level}]
     [components/widget
      {:style {:flex "1"}
       :label "Score"
       :value score}]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Piece Queue
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn piece-queue [{:keys [cell-style] :as game-opts}]
  (let [preview-grids @(rf/subscribe [::puyo.subs/preview-grids game-opts])]
    ^{:key "piece-queue"}
    [grid.views/piece-list
     {:label       "Queue"
      :piece-grids preview-grids
      :style       {:justify-content "space-between"
                    :flex-direction  "row"}
      :cell->style
      (fn [c]
        (merge
          (or cell-style {})
          {:background (color/cell->piece-color c)}))}]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Held Piece
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn hold-string [game-opts]
  (let [any-held? @(rf/subscribe [::puyo.subs/any-held? game-opts])
        hold-keys @(rf/subscribe [::subs/keys-for :hold])
        hold-key  (first hold-keys)]
    (str (if any-held? "Swap (" "Hold (") hold-key ")")))

(defn held-piece [{:keys [cell-style] :as game-opts}]
  (let [held-grid @(rf/subscribe [::puyo.subs/held-grid game-opts])]
    (grid.views/piece-list
      {:label       (hold-string game-opts)
       :piece-grids [held-grid]
       :cell->style
       (fn [{:keys [color] :as c}]
         (merge
           (or cell-style {})
           (if color
             {:background (color/cell->piece-color c)}
             {:background "transparent"})))})))

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
;; Mini-game
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mini-game
  "Intended as a div.
  Starts itself.

  Establishes sane defaults for a mini-player."
  ([] (mini-game {:name :puyo-mini-game}))
  ([game-opts]
   (let [grid      @(rf/subscribe [::puyo.subs/game-grid game-opts])
         game-opts @(rf/subscribe [::puyo.subs/game-opts game-opts])]
     [:div
      [matrix grid game-opts]])))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Full Page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page
  ([] (page {:name :puyo-page-game}))
  ([game-opts]
   (let [game-opts @(rf/subscribe [::puyo.subs/game-opts game-opts])]
     [components/page
      {:direction    :row
       :full-height? true}
      ^{:key "left"}
      [left-panel game-opts]
      ^{:key "center"}
      [center-panel game-opts]
      ^{:key "right"}
      [right-panel game-opts]
      ])))
