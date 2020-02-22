(ns games.puzzle.events
  (:require
   [re-frame.core :as rf]
   [games.events :as events]
   [games.events.interceptors :refer [game-db-interceptor]]
   [games.grid.core :as grid]))

(defn move-piece [db _direction]
  db)

(defn rotate-piece [db]
  db)

(defn step [db]
  db)

(defn add-piece
  [db {:keys [cells] :as piece}]
  (println "adding piece " piece)
  (update
    db :game-grid
    (fn [g]
      (grid/add-cells
        g {:update-cell #(assoc % :color
                                (rand-nth [:red :blue :green]))
           :make-cells  (fn [ec] (map #(grid/relative ec %) cells) )}))))

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
