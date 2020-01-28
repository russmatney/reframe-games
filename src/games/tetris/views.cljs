(ns games.tetris.views
  (:require
   [re-frame.core :as rf]
   [games.tetris.subs :as tetris.subs]
   [games.views.util :as util]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cell
  "Renders a cell as a div."
  ([c] (cell c {}))
  ([{:keys [falling occupied style] :as c} opts]
   (let [debug (:debug opts)
         width (if debug "80px" "20px")
         height (if debug "120px" "20px")]
    ^{:key (str (random-uuid))}
    [:div
     {:style
      (merge
       {:max-width width
        :max-height height
        :width width
        :height height
        :background
        (cond falling "coral"
              occupied "coral"
              true "rgba(0, 0, 0, 0.1)")
        :border
        (cond falling "rgba(0, 0, 0, 0.8) solid 1px"
              occupied "black solid 1px"
              true "rgba(0, 0, 0, 0.4) solid 1px")}
       style)}
     (if debug
      (str c)
      "")])))

(defn preview-cell
  "Renders a cell as a div."
  [{:keys [preview style] :as c} opts]
  (let [debug (:debug opts)
        width (if debug "80px" "20px")
        height (if debug "120px" "20px")]
    ^{:key (str (random-uuid))}
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

(defn matrix []
  (let [grid-data @(rf/subscribe [::tetris.subs/game-grid])]
    [:div
     {:style
      {:display "flex"
       :flex "1"
       :flex-direction "column"
       :justify-content "flex-start"
       :align-items "center"
       :margin "0 24px"}}
       ;;:border "1px solid red"}}
     (for [row grid-data]
        ^{:key (str (random-uuid))}
        [:div
         {:style
          {:display "flex"
           :transform "rotateX(0deg) rotateY(0deg) rotateZ(0deg)"}}
         (for [cell-state row]
          (cell cell-state))])]))

(defn piece-preview [grid-data]
   [:div
    (for [row grid-data]
      ^{:key (str (random-uuid))}
      [:div
       {:style {:display "flex"}}
       (for [cell-state row]
        (preview-cell cell-state {:debug false}))])])

(defn display-label
  [{:keys [label]}]
  [:h3
   {:style {:opacity "0.8"}}
   label])

(defn piece-preview-list
  ([] (piece-preview-list {}))
  ([{:keys [label piece-grids] :as metric}]
   [:div
    {:style
     {:margin-top "24px"}}
    [display-label metric]
    (for [g piece-grids]
      ^{:key (str (random-uuid))}
      [:div
       {:style
        {:display "flex"
         :justify-content "center"
         :margin-bottom "12px"}}
         ;;:border "1px solid red"}}
       (piece-preview g)])]))

(defn piece-previews []
  (let [preview-grids @(rf/subscribe [::tetris.subs/preview-grids])]
    (piece-preview-list {:label "Queue"
                         :piece-grids preview-grids})))

(defn allowed-pieces []
  (let [allowed-piece-grids @(rf/subscribe [::tetris.subs/allowed-piece-grids])]
    (piece-preview-list {:label "All pieces"
                         :piece-grids allowed-piece-grids})))

(defn held-piece []
  (let [held-grid @(rf/subscribe [::tetris.subs/held-grid])]
    (piece-preview-list {:label "Hold"
                         :piece-grids [held-grid]})))

(defn metric [{:keys [label value] :as metric}]
  [:div
   {:style {:display "flex"
            :text-align "right"
            :margin-top "24px"
            :flex-direction "column"}}

   [display-label metric]
   [:h2
    {:style {:opacity "0.9"}}
    value]])

(defn score-panel []
  (let [score @(rf/subscribe [::tetris.subs/score])
        t @(rf/subscribe [::tetris.subs/time])
        level @(rf/subscribe [::tetris.subs/level])]
    [:div.left-panel
     {:style
      {:display "flex"
       :flex "1"
       :flex-direction "column"
       :justify-content "flex-start"
       :align-items "flex-end"}}
     [metric {:label "Score" :value score}]
     [metric {:label "Time"
              :value (str (util/with-precision 1 (/ t 1000)) "s")}]
     [metric {:label "Level" :value level}]]))

(defn piece-panel []
  [:div
    {:style
      {:display "flex"
       :flex "1"
       :flex-direction "column"
       :justify-content "flex-start"
       :align-items "flex-start"}}
    [piece-previews]
    [held-piece]])
    ;;[allowed-pieces]]))

(defn page []
  [:div
   {:style
    {:height "100vh"
     :display "flex"
     :padding-top "24px"}}
   [score-panel]
   [matrix]
   [piece-panel]])
