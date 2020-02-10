(ns games.tetris.events
  (:require
   [re-frame.core :as rf]
   [games.events.interceptors :refer [game-db-interceptor]]
   [games.tetris.db :as tetris.db]
   [games.tetris.core :as tetris]
   [games.pause.core :as pause]
   [games.controls.events :as controls.events]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game loop
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::start-game
  [(game-db-interceptor ::tetris.db/db)]
  (fn [_cofx game-opts]
    {:db         (tetris.db/initial-db game-opts)
     :dispatch-n [[::set-controls game-opts]
                  [::step game-opts]]}))

(rf/reg-event-fx
  ::step
  [(game-db-interceptor ::tetris.db/db)]
  (fn [{:keys [db]} game-opts]
    (let [db (tetris/step db game-opts)]
      (if (:gameover? db)
        {:db             db
         :clear-timeouts [{:id ::step}
                          {:id ::pause/game-timer}]}
        {:db      db
         :timeout {:id    ::step
                   :event [::step game-opts]
                   :time  (:step-timeout db)}}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Set Controls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO clean up ignore-controls
(rf/reg-event-fx
  ::set-controls
  [(game-db-interceptor ::tetris.db/db)]
  (fn [{:keys [db]} {:keys [ignore-controls]}]
    (when-not ignore-controls
      {:dispatch [::controls.events/set (:controls db)]})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Move/Rotate piece
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-db
  ::move-piece
  [(game-db-interceptor ::tetris.db/db)]
  (fn [db [_game-opts direction]]
    (if (tetris/can-player-move? db)
      (tetris/move-piece db direction)
      db)))

(rf/reg-event-db
  ::rotate-piece
  [(game-db-interceptor ::tetris.db/db)]
  (fn [db [_game-opts]]
    (if (tetris/can-player-move? db)
      (tetris/rotate-piece db)
      db)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hold/Swap
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-db
  ::hold-and-swap-piece
  [(game-db-interceptor ::tetris.db/db)]
  (fn [db [_game-opts]]
    ;; if there is a hold, move current hold to front of queue
    ;; remove current falling piece from board, move it to hold
    (let [held          (:held-shape-fn db)
          falling-shape (:falling-shape-fn db)
          hold-lock     (:hold-lock db)
          paused?       (:paused? db)]
      (if
          ;; No holding if nothing falling, or if hold-lock in effect
          (or (not falling-shape)
              hold-lock
              paused?)
        db
        (cond-> db
          ;; prepend queue with held piece
          ;; TODO prevent quick double tap from stacking the queue here
          held
          (update :piece-queue (fn [q]
                                 (cons held q)))

          falling-shape
          (->
            ;; move falling piece to held piece
            (assoc :held-shape-fn falling-shape)
            ;; clear falling piece if there was one
            (assoc :falling-shape-fn nil)
            ;; clear the falling pieces from the board
            (tetris/clear-falling-cells)
            ;; update grid for showing held piece
            (update :held-grid
                    #(tetris/add-preview-piece % falling-shape))
            ;; indicate that a piece was held to prevent double-holds
            (assoc :hold-lock true)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pause
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(pause/reg-pause-events
  {:game-map-key ::tetris.db/db
   :timers       [(pause/make-timer ::step)]})
