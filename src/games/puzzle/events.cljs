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
  db)

(defn step [db]
  db)

;; TODO flag pieces being added
;; TODO clear un-set pieces when a new piece is added
(defn add-piece
  [db {:keys [cells] :as piece}]
  (update
    db :game-grid
    (fn [g]
      (-> g
          (grid/clear-cells :in-hand?)
          (grid/add-cells
            {:update-cell #(assoc % :in-hand? true)
             :cells       (map
                            (comp
                              #(assoc % :color
                                      (rand-nth [:red :blue :green]))
                              #(grid/relative {:x 1 :y 1} %))
                            cells)})))))

(events/reg-game-events
  ;; not many step features needed yet, but this initializes controls for us
  {:n       (namespace ::x)
   :step-fn step})

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
