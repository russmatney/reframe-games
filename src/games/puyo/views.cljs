(ns games.puyo.views
  (:require
   [re-frame.core :as rf]
   [games.views.components :refer [widget]]
   [games.puyo.events :as puyo.events]
   [games.puyo.subs :as puyo.subs]
   [games.puyo.views.controls :as controls]
   [games.puyo.views.about :as about]
   [games.views.util :as util]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cell->style
  [{:keys [color x y]}]
  (let [background (case color
                     :green  "#92CC41"
                     :red    "#FE493C" ;;"#B564D4"
                     :blue   "#209CEE" ;;#6ebff5
                     :yellow "#F7D51D"
                     (str "rgba(100, 100, 100, " (- 1 (/ y 10)) ")"))]
    {:background background}))

(defn cell
  ([c] (cell c {}))
  ([{:keys [x y] :as c} opts]
   (let [debug  (:debug opts)
         style  (or (:style opts) {})
         width  (if debug "260px" "40px")
         height (if debug "120px" "40px")]
     ^{:key (str x y)}
     [:div
      {:style
       (merge
         {:max-width  width
          :max-height height
          :width      width
          :height     height
          :border     "#484848 solid 1px"}
         style
         (cell->style c))}
      (if debug (str c) "")])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Grid
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn spin-the-bottle [{:keys [spin-sub grid-data pieces-sub]}]
  (let [spin?         @(rf/subscribe spin-sub)
        pieces-played (or
                        @(rf/subscribe pieces-sub)
                        0)
        reverse-x?    (even? pieces-played)
        reverse-y?    (even? pieces-played)]
    {:grid-data (if (and spin? reverse-y?)
                  (reverse grid-data)
                  grid-data)
     :row-fn    (if spin?
                  (fn [row]
                    (if reverse-x? (reverse row) row))
                  identity)}))

(defn matrix
  "Returns the rows of cells."
  []
  (let [grid-data @(rf/subscribe [::puyo.subs/game-grid])

        {:keys [grid-data row-fn]}
        (spin-the-bottle {:spin-sub   [::puyo.subs/puyo-db :spin-the-bottle?]
                          :pieces-sub [::puyo.subs/puyo-db :pieces-played]
                          :grid-data  grid-data})]

    [:div.matrix
     {:style
      {:display         "flex"
       :text-align      "center"
       :align-items     "center"
       :flex-direction  "column"
       :justify-content "center"
       :flex            "1"}}
     (for [[i row] (map-indexed vector grid-data)]
       ^{:key (str i)}
       [:div
        {:style
         {:display "flex"}}
        ;;:transform "rotateX(0deg) rotateY(0deg) rotateZ(0deg)"}}
        (for [cell-state (row-fn row)]
          (cell cell-state))])]))

(comment
  (mod 0 4))

(defn center-panel []
  (let [gameover? @(rf/subscribe [::puyo.subs/gameover?])]
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
;; Right panel

(defn piece-preview [grid-data]
  [:div
   (for [[i row] (map-indexed vector grid-data)]
     ^{:key (str i)}
     [:div
      {:style {:display "flex"}}
      (for [cell-state row]
        (cell cell-state {:style {:background "transparent"}
                          :debug false}))])])

(defn piece-preview-list
  ([] (piece-preview-list {}))
  ([{:keys [label piece-grids]}]
   [widget
    {:label label
     :style
     {:flex       "1"
      :text-align "center"}}
    (for [[i g] (map-indexed vector piece-grids)]
      ^{:key (str i)}
      [:div
       {:style
        {:display         "flex"
         :justify-content "center"
         :margin-bottom   "12px"}}
       ;;:border "1px solid red"}}
       [piece-preview g]])]))

(defn piece-previews []
  (let [preview-grids @(rf/subscribe [::puyo.subs/preview-grids])]
    (piece-preview-list {:label       "Queue"
                         :piece-grids preview-grids})))

(defn held-piece []
  (let [held-grid   @(rf/subscribe [::puyo.subs/held-grid])
        any-held?   @(rf/subscribe [::puyo.subs/any-held?])
        hold-keys   @(rf/subscribe [::puyo.subs/keys-for :hold])
        hold-key    (first hold-keys)
        hold-string (str (if any-held? "Swap (" "Hold (") hold-key ")")]
    (piece-preview-list {:label       hold-string
                         :piece-grids [held-grid]})))


(defn controls-and-about []
  (let [pause-keys    @(rf/subscribe [::puyo.subs/keys-for :pause])
        pause-key     (first pause-keys)
        controls-keys @(rf/subscribe [::puyo.subs/keys-for :controls])
        controls-key  (first controls-keys)
        about-keys    @(rf/subscribe [::puyo.subs/keys-for :about])
        about-key     (first about-keys)
        rotate-keys   @(rf/subscribe [::puyo.subs/keys-for :rotate])
        rotate-key    (first rotate-keys)]
    [widget
     {:style
      {:padding "0.9rem"
       :flex    "1"}}
     ^{:key "rotate"}
     [:p
      {:style    {:margin-bottom "0.3rem"}
       :on-click #(rf/dispatch [::puyo.events/rotate-piece])}
      (str "Rotate (" rotate-key ")")]
     ^{:key "pause"}
     [:p
      {:style    {:margin-bottom "0.3rem"}
       :on-click #(rf/dispatch [::puyo.events/toggle-pause])}
      (str "Pause (" pause-key ")")]
     ^{:key "controls"}
     [:p
      {:style    {:margin-bottom "0.3rem"}
       :on-click #(rf/dispatch [::puyo.events/set-view :controls])}
      (str "Controls (" controls-key ")")]
     ^{:key "about"}
     [:p
      {:style    {:margin-bottom "0.3rem"}
       :on-click #(rf/dispatch [::puyo.events/set-view :about])}
      (str "About (" about-key ")")]]))

(defn right-panel []
  [:div
   {:style
    {:display        "flex"
     :flex           "1"
     :flex-direction "column"}}
   [piece-previews]
   [held-piece]
   [controls-and-about]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main page component

(def background-color "#441086")

(defn page []
  (let [current-view @(rf/subscribe [::puyo.subs/current-view])
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
     (for [c comps] c)]))
