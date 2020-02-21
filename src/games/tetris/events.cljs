(ns games.tetris.events
  (:require
   [games.tetris.core :as tetris]
   [games.events :as events]))

;; register game events
(events/reg-game-events
  {:n       (namespace ::x)
   :step-fn tetris/step})

(events/reg-game-move-events
  {:n                  (namespace ::x)
   :can-player-move?   tetris/can-player-move?
   :move-piece         tetris/move-piece
   :instant-fall       tetris/instant-fall
   :after-piece-played tetris/after-piece-played
   :rotate-piece       tetris/rotate-piece})

(events/reg-hold-event
  {:n                   (namespace ::x)
   :can-player-move?    tetris/can-player-move?
   :clear-falling-cells tetris/clear-falling-cells
   :add-preview-piece   tetris/add-preview-piece})
