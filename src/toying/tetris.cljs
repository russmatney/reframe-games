(ns toying.tetris)

(def game-state
  {:grid
   [[0 0 0 0 0]
    [0 0 0 0 0]
    [0 0 0 0 0]
    [0 0 0 0 0]
    [0 0 0 0 0]
    [0 0 0 0 0]
    [0 0 0 0 0]
    [0 0 0 0 0]
    [0 0 0 0 0]
    [0 0 0 0 0]]})

(defn stage []
  [:div
   (for [row (:grid game-state)]
     [:div
      {:style
       {:display "flex"}}
      (for [cell row]
        [:div
         {:style
          {:max-width "30px"
           :max-height "30px"
           :width "30px"
           :height "30px"
           :background "powderblue"
           :border "black solid 1px"}}
         ""])])])

