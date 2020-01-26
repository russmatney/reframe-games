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

(defn piece-preview []
  (let [grid-data @(rf/subscribe [::tetris.subs/preview-grid])]
    [:div
     [:h2 "Next piece"]
     [:div
      (for [row grid-data]
        ^{:key (str (random-uuid))}
        [:div
         {:style {:display "flex"}}
         (for [cell-state row]
          (preview-cell cell-state {:debug false}))])]]))

(defn with-precision [p num]
  (let [num (or num 0)]
    (.toFixed num p)))

(comment
  (with-precision 4 195.556)
  (with-precision 2 0))

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
     ;; TODO fix this g.d. timer to feel real
     [:h2 (with-precision 2 (/ t 1000))]
     [:h2 "Level"]
     [:h2 level]]
    [grid]
    [piece-preview]]))
