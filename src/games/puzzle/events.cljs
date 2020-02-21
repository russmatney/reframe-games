(ns games.puzzle.events
  (:require
   [games.events :as events]
   [games.puzzle.core :as puzzle]))

(events/reg-game-events
  {:n       (namespace ::x)
   :step-fn puzzle/step})

(events/reg-game-move-events
  {:n            (namespace ::x)
   :move-piece   puzzle/move-piece
   :rotate-piece puzzle/rotate-piece})
