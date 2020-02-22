(ns games.puzzle.subs
  (:require
   [re-frame.core :as rf]
   [games.grid.core :as grid]))

(defn ->grid
  [cells]
  (-> {:height 5 :width 5}
      (grid/build-grid)
      (grid/add-cells
        {:update-cell #(assoc % :color
                              (rand-nth [:red :blue :green]))
         :cells       (map #(grid/relative {:x 1 :y 1} %) cells)})))

(rf/reg-sub
  ::piece-grids
  (fn [db [_ game-opts]]
    (let [piece-cells
          (-> db :games (get (:name game-opts)) :pieces)]
      (map
        (fn [{:keys [cells] :as piece}]
          [piece (->grid cells)])
        piece-cells))))
