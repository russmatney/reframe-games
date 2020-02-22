(ns games.puzzle.subs
  (:require
   [re-frame.core :as rf]
   [games.grid.core :as grid]))

(defn choose-color []
  (rand-nth [:red :blue :green]))

(defn cells->ec->cells
  [cells]
  (fn [ec]
    (map (comp
           #(assoc % :color (choose-color))
           #(grid/relative ec %)) cells)))

(defn ->grid
  [cells]
  (-> {:height     5 :width 5
       :entry-cell {:x 1 :y 1}}
      (grid/build-grid)
      (grid/add-cells
        {:make-cells (cells->ec->cells cells)})))

(rf/reg-sub
  ::piece-grids
  (fn [db [_ game-opts]]
    (let [piece-cells (-> db :games (get (:name game-opts)) :pieces)]
      (map ->grid piece-cells))))
