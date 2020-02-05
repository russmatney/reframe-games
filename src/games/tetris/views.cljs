(ns games.tetris.views
  (:require
   [re-frame.core :as rf]
   [games.tetris.subs :as tetris.subs]
   [games.tetris.events :as tetris.events]
   [games.tetris.views.components :refer [widget display-label]]
   [games.tetris.views.controls :as controls]
   [games.tetris.views.about :as about]
   [games.views.util :as util]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cell
  "Renders a cell as a div."
  ([c] (cell c {}))
  ([{:keys [falling occupied style x y] :as c} opts]
   (let [debug (:debug opts)
         width (if debug "80px" "20px")
         height (if debug "120px" "20px")]
    ^{:key (str x y)}
    [:div
     {:style
      (merge
       {:max-width width
        :max-height height
        :width width
        :height height
        :border "#484848 solid 1px"}
       style)}
     (if debug
      (str c)
      "")])))

(defn preview-cell
  "Renders a cell as a div."
  [{:keys [preview style x y] :as c} opts]
  (let [debug (:debug opts)
        width (if debug "80px" "20px")
        height (if debug "120px" "20px")]
    ^{:key (str x y)}
    [:div
     {:style
      (merge
       {:max-width width
        :max-height height
        :width width
        :height height
        :background (when preview "coral")
        :border (if preview
                  "black solid 1px"
                  ;;"gray solid 1px"
                  "transparent solid 1px")}
       style)}
     (if debug
      (str c)
      "")]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Grid
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn matrix
  "Returns the rows of cells."
  []
  (let [grid-data @(rf/subscribe [::tetris.subs/game-grid])]
    (for [[i row] (map-indexed vector grid-data)]
       ^{:key (str i)}
       [:div
        {:style
         {:display "flex"}}
          ;;:transform "rotateX(0deg) rotateY(0deg) rotateZ(0deg)"}}
        (for [cell-state row]
         (cell cell-state))])))

(defn center-panel []
  (let [gameover? @(rf/subscribe [::tetris.subs/gameover?])
        children
        (if gameover?
          (concat
           [^{:key "go"}
            [:h3
             {:style {:margin-bottom "1rem"}}
             "Game Over."]]
           (matrix)
           [^{:key "rest."}
            [:p
             {:style {:margin-top "1rem"}
              :on-click #(rf/dispatch [::tetris.events/start-game])}
             "Click here to restart."]])
          (matrix))]
    [:div.center-panel
     {:style
      {:display "flex"
       :flex "1"}}
     [widget
       {:style
        {:flex "1"}
        :children
        children}]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Left panel

(defn left-panel []
  (let [score @(rf/subscribe [::tetris.subs/score])
        t @(rf/subscribe [::tetris.subs/time])
        level @(rf/subscribe [::tetris.subs/level])
        paused? @(rf/subscribe [::tetris.subs/paused?])
        time (str (util/with-precision 1 (/ t 1000)) "s")]
    [:div.left-panel
     {:style
      {:display "flex"
       :flex "1"
       :flex-direction "column"}}
     [widget
      {:on-click #(rf/dispatch [::tetris.events/toggle-pause])
       :style
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
;; Right panel

(defn piece-preview [grid-data]
   [:div
    (for [[i row] (map-indexed vector grid-data)]
      ^{:key (str i)}
      [:div
       {:style {:display "flex"}}
       (for [cell-state row]
        (preview-cell cell-state {:debug false}))])])

(defn piece-preview-list
  ([] (piece-preview-list {}))
  ([{:keys [label piece-grids]}]
   [widget
    {:label label
     :style
     {:flex "1"
      :text-align "center"}
     :children
     (for [[i g] (map-indexed vector piece-grids)]
       ^{:key (str i)}
       [:div
        {:style
         {:display "flex"
          :justify-content "center"
          :margin-bottom "12px"}}
          ;;:border "1px solid red"}}
        (piece-preview g)])}]))

(defn piece-previews []
  (let [preview-grids @(rf/subscribe [::tetris.subs/preview-grids])]
    (piece-preview-list {:label "Queue"
                         :piece-grids preview-grids})))

(defn allowed-pieces []
  (let [allowed-piece-grids @(rf/subscribe [::tetris.subs/allowed-piece-grids])]
    (piece-preview-list {:label "All pieces"
                         :piece-grids allowed-piece-grids})))

(defn held-piece []
  (let [held-grid @(rf/subscribe [::tetris.subs/held-grid])
        any-held? @(rf/subscribe [::tetris.subs/any-held?])
        hold-keys @(rf/subscribe [::tetris.subs/keys-for :hold])
        hold-key (first hold-keys)
        hold-string (str (if any-held? "Swap (" "Hold (") hold-key ")")]
    (piece-preview-list {:label hold-string
                         :piece-grids [held-grid]})))


(defn controls-and-about []
  (let [pause-keys @(rf/subscribe [::tetris.subs/keys-for :pause])
        pause-key (first pause-keys)
        controls-keys @(rf/subscribe [::tetris.subs/keys-for :controls])
        controls-key (first controls-keys)
        about-keys @(rf/subscribe [::tetris.subs/keys-for :about])
        about-key (first about-keys)
        rotate-keys @(rf/subscribe [::tetris.subs/keys-for :rotate])
        rotate-key (first rotate-keys)]
    [widget
     {:style
      {:padding "0.9rem"
       :flex "1"}
      :children
      [^{:key "rotate"}
       [:p
        {:style {:margin-bottom "0.3rem"}
         :on-click #(rf/dispatch [::tetris.events/rotate-piece])}
        (str "Rotate (" rotate-key ")")]
       ^{:key "pause"}
       [:p
        {:style {:margin-bottom "0.3rem"}
         :on-click #(rf/dispatch [::tetris.events/toggle-pause])}
        (str "Pause (" pause-key ")")]
       ^{:key "controls"}
       [:p
        {:style {:margin-bottom "0.3rem"}
         :on-click #(rf/dispatch [::tetris.events/set-view :controls])}
        (str "Controls (" controls-key ")")]
       ^{:key "about"}
       [:p
        {:style {:margin-bottom "0.3rem"}
         :on-click #(rf/dispatch [::tetris.events/set-view :about])}
        (str "About (" about-key ")")]]}]))

(defn right-panel []
  [:div
    {:style
      {:display "flex"
       :flex "1"
       :flex-direction "column"}}
    [piece-previews]
    [held-piece]
    [controls-and-about]])
    ;;[allowed-pieces]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main page component

(def background-color "#441086")
     ;;:background "#5d08c7"

(defn page []
  (let [current-view @(rf/subscribe [::tetris.subs/current-view])
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
