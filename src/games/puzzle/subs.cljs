(ns games.puzzle.subs
  (:require
   [re-frame.core :as rf]
   [games.grid.core :as grid]))

(defn cells->ec->cells
  [cells]
  (fn [ec]
    (map #(grid/relative ec %) cells)))

(defn ->grid
  [cells]
  (-> {:height     5 :width 5
       :entry-cell {:x 1 :y 1}}
      (grid/build-grid)
      (grid/add-cells
        {:update-cell #(assoc % :color
                              (rand-nth [:red :blue :green]))
         :make-cells  (cells->ec->cells cells)})))

(rf/reg-sub
  ::piece-grids
  (fn [db [_ game-opts]]
    (let [piece-cells
          (-> db :games (get (:name game-opts)) :pieces)]
      (map
        (fn [{:keys [cells] :as piece}]
          [piece (->grid cells)])
        piece-cells))))
