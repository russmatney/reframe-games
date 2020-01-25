(ns games.tetris.views
  (:require
   [re-frame.core :as rf]
   [games.subs :as subs]))

;; TODO move to games.tetris.views?
;; TODO show coming pieces
;; TODO show score
;; TODO add color to pieces
;; TODO do styles via ui?
;; TODO add tool for collecting feedback?

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cell
  "Renders a cell as a div."
  ([c] (cell c {}))
  ([{:keys [falling occupied] :as c} opts]
   (let [debug (:debug opts)
         width (if debug "80px" "20px")
         height (if debug "120px" "20px")]
    ^{:key (str (random-uuid))}
    [:div
     {:style
      {:max-width width
       :max-height height
       :width width
       :height height
       :background (cond falling "coral"
                         occupied "gray"
                         true "powderblue")
       :border "black solid 1px"}}
     (if debug
      (str c)
      "")])))

(defn preview-cell
  "Renders a cell as a div."
  [{:keys [preview] :as c} opts]
  (let [debug (:debug opts)
        width (if debug "80px" "20px")
        height (if debug "120px" "20px")]
    ^{:key (str (random-uuid))}
    [:div
     {:style
      {:max-width width
       :max-height height
       :width width
       :height height
       :background (when preview "coral")
       :border (if preview
                 "black solid 1px"
                 "transparent solid 1px")}}
     (if debug
      (str c)
      "")]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Grid
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn grid []
  (let [grid-data @(rf/subscribe [::subs/game-grid])]
    [:div
      (for [row grid-data]
        ^{:key (str (random-uuid))}
        [:div
         {:style {:display "flex"}}
         (for [cell-state row]
          (cell cell-state))])]))

(defn piece-preview []
  (let [grid-data @(rf/subscribe [::subs/preview-grid])]
    [:div
     [:h2 "Next piece"]
     [:div
      (for [row grid-data]
        ^{:key (str (random-uuid))}
        [:div
         {:style {:display "flex"}}
         (for [cell-state row]
          (preview-cell cell-state {:debug false}))])]]))
