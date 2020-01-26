(ns games.tetris.views
  (:require
   [re-frame.core :as rf]
   [games.tetris.subs :as tetris.subs]))

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
        :background (cond falling "coral"
                          occupied "coral"
                          true "gray")
        :border "black solid 1px"}
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
                  "transparent solid 1px")}
       style)}
     (if debug
      (str c)
      "")]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Grid
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn grid []
  (let [grid-data @(rf/subscribe [::tetris.subs/game-grid])]
    [:div
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

(defn piece-preview-list
  ([] (piece-preview-list {}))
  ([{:keys [label piece-grids]}]
   [:div
    [:h2 label]
    (for [g piece-grids]
      ^{:key (str (random-uuid))}
      [:div
       {:style {:display "flex"}}
       (piece-preview g)])]))

(defn piece-previews []
  (let [preview-grids @(rf/subscribe [::tetris.subs/preview-grids])]
    (piece-preview-list {:label "Next pieces"
                         :piece-grids preview-grids})))

(defn allowed-pieces []
  (let [allowed-piece-grids @(rf/subscribe [::tetris.subs/allowed-piece-grids])]
    (piece-preview-list {:label "All pieces"
                         :piece-grids allowed-piece-grids})))

(defn held-piece []
  (let [held-grid @(rf/subscribe [::tetris.subs/held-grid])]
    (piece-preview-list {:label "Held"
                         :piece-grids [held-grid]})))

(defn with-precision [p num]
  (let [num (or num 0)]
    (.toFixed num p)))

(defn page []
  (let [score @(rf/subscribe [::tetris.subs/score])
        t @(rf/subscribe [::tetris.subs/time])
        level @(rf/subscribe [::tetris.subs/level])]
   [:div
    {:style
     {:display "flex"
      :margin "10px"}}
    [:div
     [:h2 "Score"]
     [:h2 score]
     [:h2 "Time"]
     [:h2 (str (with-precision 1 (/ t 1000)) "s")]
     [:h2 "Level"]
     [:h2 level]]
    [grid]
    [held-piece]
    [piece-previews]
    [allowed-pieces]]))
