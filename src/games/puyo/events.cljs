(ns games.puyo.events
  (:require
   [re-frame.core :as rf]
   [games.events.interceptors :refer [game-db-interceptor]]
   [games.puyo.db :as puyo.db]
   [games.puyo.core :as puyo]
   [games.pause.core :as pause]
   [games.controls.events :as controls.events]
   [games.puyo.shapes :as puyo.shapes]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game loop
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::init-game
  [(game-db-interceptor)]
  (fn [_cofx game-opts]
    {:dispatch-n [[::controls.events/register-controls game-opts]
                  [::step game-opts]
                  [::game-timer game-opts]]}))

(rf/reg-event-fx
  ::stop-game
  [(game-db-interceptor)]
  (fn [_cofx game-opts]
    {:dispatch-n     [[::controls.events/deregister-controls game-opts]]
     :clear-timeouts [{:id ::step}
                      {:id ::game-timer}]}))

(rf/reg-event-fx
  ::restart-game
  [(game-db-interceptor)]
  (fn [_cofx game-opts]
    {:db         (puyo.db/game-dbs-map (:name game-opts))
     :dispatch-n [[::step game-opts]
                  [::game-timer game-opts]]}))

(rf/reg-event-fx
  ::step
  [(game-db-interceptor)]
  (fn [{:keys [db]} game-opts]
    (let [{:keys [step-timeout] :as db} (puyo/step db game-opts)]
      (if (:gameover? db)
        {:db             db
         :clear-timeouts [{:id ::step}
                          {:id ::game-timer}]}
        {:db      db
         :timeout {:id    ::step
                   :event [::step game-opts]
                   :time  step-timeout}}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Move/Rotate piece
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO update interceptor to handle this (pass game-opts through kbd events)
(rf/reg-event-db
  ::move-piece
  [(game-db-interceptor)]
  (fn [db [_game-opts direction]]
    (if (puyo/can-player-move? db)
      (puyo/move-piece db direction)
      db)))

(rf/reg-event-db
  ::instant-fall
  [(game-db-interceptor)]
  (fn [db [_game-opts direction]]
    (if (puyo/can-player-move? db)
      (-> db
          (puyo/instant-fall direction)
          (puyo/after-piece-played))
      db)))

(rf/reg-event-db
  ::rotate-piece
  [(game-db-interceptor)]
  (fn [db _game-opts]
    (if (puyo/can-player-move? db)
      (puyo/rotate-piece db)
      db)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hold/Swap
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO dry up hold/swap logic?
(rf/reg-event-db
  ::hold-and-swap-piece
  [(game-db-interceptor)]
  (fn [db _game-opts]
    (let [{:keys [held-shape falling-shape hold-lock paused?]} db]
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
            (puyo/clear-falling-cells)
            ;; update grid for showing held piece
            (update :held-grid
                    #(puyo/add-preview-piece
                       % (puyo.shapes/build-piece-fn falling-shape)))

            ;; indicate that a piece was held to prevent double-holds
            (assoc :hold-lock true)

            ;; TODO handle this better!
            ;; decrement current-piece-num (it's about be re-inc in add-new-piece)
            ;; NOTE that current-piece-num excludes holds to supports combos
            (update :current-piece-num dec)))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pause
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(pause/reg-pause-events
  {:n      :games.puyo.events
   :timers [{:game-opts->id    (fn [_] ::step)
             :game-opts->event (fn [gopts] [::step gopts])}]})
