(ns games.puzzle.views
  (:require [games.grid.views :as grid.views]
            [games.subs :as subs]
            [re-frame.core :as rf]
            [games.views.components :as components]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Select game
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn select-game-cells
  [{:keys [placed? x y]}]
  ^{:key (str x y)}
  [:div
   {:style
    {:height     "48px"
     :width      "48px"
     :border     (if placed? "1px solid white" "1px solid black")
     :background (if placed? "green" "white")}}
   ""])

(defn select-game
  []
  (let [game-opts {:name :puzzle-select-game}
        grid      @(rf/subscribe [::subs/game-grid game-opts])]
    [grid.views/matrix grid
     {:->cell select-game-cells}]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page
  []
  [components/page
   {}
   [:div "puzzle"]])
