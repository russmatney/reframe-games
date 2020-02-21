(ns games.debug.events
  (:require
   [re-frame.core :as rf]
   [games.events.interceptors :refer [game-db-interceptor]]
   [games.debug.core :as debug]
   [games.controls.events :as controls.events]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Debug 'game'
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO dry up with events/reg-game-events (fix circular dep)

(rf/reg-event-fx
  ::init-game
  [(game-db-interceptor)]
  (fn [{:keys [_db]} game-opts]
    {:dispatch [::controls.events/register-controls game-opts]}))

(rf/reg-event-fx
  ::stop-game
  [(game-db-interceptor)]
  (fn [{:keys [_db]} game-opts]
    {:dispatch [::controls.events/deregister-controls game-opts]}))

(rf/reg-event-db
  ::add-piece
  [(game-db-interceptor)]
  (fn [db _game-opts]
    (debug/add-pieces db)))

(rf/reg-event-db
  ::move-piece
  [(game-db-interceptor)]
  (fn [db [_game-opts direction]]
    (if (debug/move-allowed? db)
      (debug/move-piece db direction)
      db)))

(rf/reg-event-db
  ::instant-fall
  [(game-db-interceptor)]
  (fn [db [_game-opts direction]]
    (if (debug/move-allowed? db)
      (debug/instant-fall db direction)
      db)))

(rf/reg-event-db
  ::rotate-piece
  [(game-db-interceptor)]
  (fn [db _game-opts]
    (if (debug/move-allowed? db)
      (debug/rotate-piece db)
      db)))

(rf/reg-event-db
  ::toggle-debug
  [(game-db-interceptor)]
  (fn [db _game-opts]
    (update-in db [:game-opts :debug?] not)))
