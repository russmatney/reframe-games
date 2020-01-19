(ns toying.views
  (:require
   [reagent.core :as reagent]
   [toying.tetris :as tetris]))

(defn timer-component []
  (let [seconds-elapsed (reagent/atom 0)]
    (fn []
      (js/setTimeout #(swap! seconds-elapsed inc) 1000)
      [:div "Seconds Elapsed: " @seconds-elapsed])))

(defn root []
  [:div#root
   [timer-component]
   [tetris/stage]])

