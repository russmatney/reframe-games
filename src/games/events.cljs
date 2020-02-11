(ns games.events
  (:require
   [re-frame.core :as rf]
   [games.tetris.events :as tetris.events]
   [games.puyo.events :as puyo.events]
   [games.controls.events :as controls.events]
   [games.db :as db]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Init db
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO cofx to read defaults from user's config/cookie
;; ex: default-page
(rf/reg-event-db
  ::init-db
  (fn [_] (db/initial-db)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Start Games
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Dispatch start-games events into game event modules
(rf/reg-event-fx
  ::start-games
  (fn [_cofx]
    {:dispatch-n
     [[::puyo.events/start-games]
      [::tetris.events/start-games]
      [::controls.events/start-games]]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Navigation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::set-page
  (fn [{:keys [db]} [_ page]]
    {:db (assoc db :current-page page)}))

;; TODO update to dispatch broader start/stop games events
;; might even dispatch navigation events to game event modules
(rf/reg-event-fx
  ::unset-page
  (fn [{:keys [db]} _]
    (let [current-page (-> db :current-page)]
      (cond-> {:db (dissoc db :current-page)}
        (contains? #{:tetris :puyo} current-page)
        (assoc :dispatch
               (case current-page
                 :tetris [::tetris.events/pause-game]
                 :puyo   [::puyo.events/pause-game]))))))
