(ns games.debug.events
  (:require
   [re-frame.core :as rf]
   [games.events.interceptors :refer [game-db-interceptor]]
   [games.debug.core :as debug]
   [games.events :as events]))

;; register game events
(events/reg-game-events
  {:n       (namespace ::x)
   ;; TODO handle no-step cases, optional timers
   :step-fn identity})

(events/reg-game-move-events
  {:n            (namespace ::x)
   :move-piece   debug/move-piece
   :instant-fall debug/instant-fall
   :rotate-piece debug/rotate-piece})

(rf/reg-event-db
  ::add-piece
  [(game-db-interceptor)]
  (fn [db _game-opts]
    (debug/add-pieces db)))

(rf/reg-event-db
  ::toggle-debug
  [(game-db-interceptor)]
  (fn [db _game-opts]
    (update-in db [:game-opts :debug?] not)))
