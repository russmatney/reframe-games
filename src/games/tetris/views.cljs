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
