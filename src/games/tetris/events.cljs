(ns games.tetris.events
  (:require
   [re-frame.core :as rf]
   [games.events.interceptors :refer [game-db-interceptor]]
   [games.tetris.core :as tetris]
   [games.pause.core :as pause]
   [games.controls.events :as controls.events]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game loop
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::init-game
  [(game-db-interceptor)]
  (fn [_cofx game-opts]
    {:dispatch-n [[::register-controls game-opts]
                  [::step game-opts]
                  [::game-timer game-opts]]}))

(rf/reg-event-fx
  ::step
  [(game-db-interceptor)]
  (fn [{:keys [db]} game-opts]
    (let [db (tetris/step db game-opts)]
      (if (:gameover? db)
        {:db             db
         :clear-timeouts [{:id ::step}
                          {:id ::game-timer}]}
        {:db      db
         :timeout {:id    ::step
                   :event [::step game-opts]
                   :time  (:step-timeout db)}}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Set Controls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO clean up ignore-controls
(rf/reg-event-fx
  ::register-controls
  [(game-db-interceptor)]
  (fn [{:keys [db]} {:keys [ignore-controls]}]
    (when-not ignore-controls
      {:dispatch [::controls.events/register (:controls db)]})))

(rf/reg-event-fx
  ::deregister-controls
  [(game-db-interceptor)]
  (fn [{:keys [db]} _game-opts]
    {:dispatch
     [::controls.events/deregister (:controls db)]}))

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

;; TODO fix interceptor for these events
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
  (fn [db _game-opts]
    ;; if there is a hold, move current hold to front of queue
    ;; remove current falling piece from board, move it to hold
    (let [held          (:held-shape db)
          falling-shape (:falling-shape db)
          hold-lock     (:hold-lock db)
          paused?       (:paused? db)]
      (if ;; No holding if nothing falling, or if hold-lock in effect
          (or (not falling-shape)
              hold-lock
              paused?)
        db
        (cond-> db
          ;; prepend queue with held piece
          held
          (update :piece-queue
                  (fn [q] (cons held q)))

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
            (assoc :hold-lock true)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pause
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(pause/reg-pause-events
  {:n      :games.tetris.events
   :timers [{:game-opts->id    (fn [_] ::step)
             :game-opts->event (fn [gopts] [::step gopts])}]})
