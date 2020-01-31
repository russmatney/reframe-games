(ns games.tetris.views.components)



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Shared Utils

(defn display-label
  [label]
  [:h3
   {:style
    {:opacity "0.9"
     :margin-bottom "12px"}}
   label])

(defn widget
  ([{:keys [label value style children]}]
   [:div
    {:class "nes-container is-dark"
     :style
     (merge
      {:display "flex"
       :flex "1"
       :text-align "center"
       :align-items "center"
       :flex-direction "column"
       :justify-content "center"}
      style)}

    (when label [display-label label])
    (when value
      [:h2
       {:style {:opacity "0.95"}}
       value])
    (when children
      (for [c children] c))]))
