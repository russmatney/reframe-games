(ns games.puyo.events
  (:require
   [games.events :as events]
   [games.puyo.core :as puyo]))

;; register game events
(events/reg-game-events
  {:n       (namespace ::x)
   :step-fn puyo/step})

(events/reg-game-move-events
  {:n                  (namespace ::x)
   :can-player-move?   puyo/can-player-move?
   :move-piece         puyo/move-piece
   :instant-fall       puyo/instant-fall
   :after-piece-played puyo/after-piece-played
   :rotate-piece       puyo/rotate-piece})

(events/reg-hold-event
  {:n                   (namespace ::x)
   :can-player-move?    puyo/can-player-move?
   :clear-falling-cells puyo/clear-falling-cells
   :add-preview-piece   puyo/add-preview-piece
   :on-hold             (fn [db] (update db :current-piece-num dec))})
