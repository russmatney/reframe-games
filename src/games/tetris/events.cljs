(ns games.tetris.events
  (:require
   [re-frame.core :as rf]
   [games.events.interceptors :refer [game-db-interceptor]]
   [games.tetris.core :as tetris]
   [games.events :as events]))

;; register game events
(events/reg-game-events
  {:n       (namespace ::x)
   :step-fn tetris/step})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Move/Rotate piece
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-db
  ::move-piece
  [(game-db-interceptor)]
  (fn [db [_game-opts direction]]
    (if (tetris/can-player-move? db)
      (tetris/move-piece db direction)
      db)))

(rf/reg-event-db
  ::instant-fall
  [(game-db-interceptor)]
  (fn [db [_game-opts direction]]
    (if (tetris/can-player-move? db)
      (-> db
          (tetris/instant-fall direction)
          (tetris/after-piece-played))
      db)))

(rf/reg-event-db
  ::rotate-piece
  [(game-db-interceptor)]
  (fn [db _game-opts]
    (if (tetris/can-player-move? db)
      (tetris/rotate-piece db)
      db)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hold/Swap
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-db
  ::hold-and-swap-piece
  [(game-db-interceptor)]
  (fn [{:keys [held-shape falling-shape hold-lock paused?]
        :as   db
        } _game-opts]
    (if (or (not falling-shape)
            hold-lock
            paused?)
      db
      (cond-> db
        ;; prepend queue with held piece
        held-shape
        (update :piece-queue (fn [q]
                               (cons held-shape q)))

        falling-shape
        (->
          ;; move falling piece to held piece
          (assoc :held-shape falling-shape)
          ;; clear falling piece if there was one
          (assoc :falling-shape nil)
          ;; clear the falling pieces from the board
          (tetris/clear-falling-cells)
          ;; update grid for showing held piece
          (update :held-grid
                  #(tetris/add-preview-piece % falling-shape))

          ;; indicate that a piece was held to prevent double-holds
          (assoc :hold-lock true))))))
