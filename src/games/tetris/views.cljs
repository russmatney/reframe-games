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

(defn cell [{:keys [falling occupied] :as cell}]
  (let [debug false
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
     (str cell)
     "")]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Grid
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn grid []
  (let [grid-data @(rf/subscribe [::subs/grid-for-display])]
   [:div
     (for [row grid-data]
       ^{:key (str (random-uuid))}
       [:div
        {:style {:display "flex"}}
        (for [cell-state row]
         (cell cell-state))])]))
