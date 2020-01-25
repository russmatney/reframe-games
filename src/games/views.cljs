(ns games.views
  (:require
   [reagent.core :as reagent]
   [games.tetris.views :as tetris.views]))

(defn timer-component []
  (let [seconds-elapsed (reagent/atom 0)]
    (fn []
      (js/setTimeout #(swap! seconds-elapsed inc) 1000)
      [:div "Seconds Elapsed: " @seconds-elapsed])))

(defn root []
  [:div#root
   {:style {:display "flex"}}
   [timer-component]
   [:div
    {:style {:display "flex" :margin "10px"}}
    [tetris.views/grid]]

   [:div
    {:style {:display "flex" :margin "10px"}}
    [tetris.views/alt-grid]]])


