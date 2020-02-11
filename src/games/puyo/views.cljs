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
   [games.puyo.subs :as puyo.subs]))

;; Write a metadata component
;;
;; include: current combo, highest combo, combos to next level, highest level
;; pieces played, combos scored, items available

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO rewrite into 'shapes' style?
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
  ([{:keys [cell-style] :as game-opts}]
   (let [grid          @(rf/subscribe [::puyo.subs/game-grid game-opts])
         spin?         @(rf/subscribe [::puyo.subs/puyo-db game-opts :spin-the-bottle?])
         pieces-played @(rf/subscribe [::puyo.subs/puyo-db game-opts :pieces-played])

         grid
         (cond-> grid
           spin?
           (grid/spin {:reverse-y? (contains? #{1 2 3} (mod pieces-played 6))
                       :reverse-x? (contains? #{2 3 4} (mod pieces-played 6))}))]
     [grid.views/matrix
      grid
      {:cell->style
       (fn [{:keys [color] :as c}]
         (merge
           (or cell-style {})
           (if color
             {:background (cell->piece-color c)}
             {:background (cell->background c)})))}])))

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
  (let [gameover? @(rf/subscribe [::puyo.subs/gameover? game-opts])]
    [:div.center-panel
     {:style
      {:display "flex"
       :flex    "1"}}
     [components/widget
      {:style {:flex "1"}}

      (when gameover? [gameover])
      ^{:key "matrix"} [matrix game-opts]
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
          {:background (cell->piece-color c)}))}]))

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
             {:background (cell->piece-color c)}
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

(def page-game-defaults
  {:name        :default
   :cell-style  {:width "20px" :height "20px"}
   :no-walls-x? true
   :game-grid   {:entry-cell {:x 3 :y -1}
                 :height     16
                 :width      8}})

(defn page
  ([] (page {}))
  ([game-opts]
   (let [game-opts (merge page-game-defaults game-opts)]
     (rf/dispatch [::puyo.events/start-game game-opts])
     [components/page
      {:direction    :row
       :full-height? true}
      [left-panel game-opts]
      [center-panel game-opts]
      [right-panel game-opts]
      ])))
