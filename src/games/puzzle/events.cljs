(ns games.puzzle.events
  (:require
   [games.events :as events]))

(defn move-piece [db _direction]
  db)

(defn rotate-piece [db]
  db)

(defn step [db]
  db)

(events/reg-game-events
  ;; not many step features needed yet, but this initializes controls for us
  {:n       (namespace ::x)
   :step-fn step})

(events/reg-game-move-events
  ;; connects a few controls to functions for us
  {:n            (namespace ::x)
   :move-piece   move-piece
   :rotate-piece rotate-piece})
