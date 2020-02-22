(ns games.puzzle.events
  (:require
   [re-frame.core :as rf]
   [games.events :as events]
   [games.events.interceptors :refer [game-db-interceptor]]
   [games.grid.core :as grid]))

(defn move-piece [db direction]
  (update
    db :game-grid
    #(grid/move-cells % {:cells :in-hand? :direction direction})))

(defn rotate-piece [db]
  (update
    db :game-grid
    (fn [g]
      (grid/move-cells
        g {:cells :in-hand? :rotation :clockwise}))))

(defn add-piece
  [db {:keys [cells]}]
  (update
    db :game-grid
    (fn [g]
      (-> g
          (grid/clear-cells :in-hand?)
          (grid/add-cells
            {:update-cell #(assoc % :in-hand? true)
             :cells
             (map
               (comp
                 #(assoc % :color (rand-nth [:red :blue :green]))
                 #(grid/relative {:x 1 :y 1} %))
               cells)})))))

(defn set-piece
  [db]
  (update
    db :game-grid
    (fn [g]
      (grid/update-cells
        g
        :in-hand?
        (fn [c]
          (-> c
              (dissoc :in-hand?)
              (assoc :set? true)))))))

(events/reg-game-events
  ;; no step features needed, but this initializes controls for us
  {:n       (namespace ::x)
   :step-fn identity})

(events/reg-game-move-events
  ;; connects a few controls to functions for us
  {:n            (namespace ::x)
   :move-piece   move-piece
   :rotate-piece rotate-piece})

(rf/reg-event-db
  ::select-piece
  [(game-db-interceptor)]
  (fn [db [_game-opts piece]]
    (add-piece db piece)))

(rf/reg-event-db
  ::set-piece
  [(game-db-interceptor)]
  (fn [db _game-opts]
    (set-piece db)))
