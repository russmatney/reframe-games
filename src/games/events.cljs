(ns games.events
  (:require
   [re-frame.core :as rf]
   [games.tetris.events :as tetris.events]
   [games.puyo.events :as puyo.events]
   [games.db :as db]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Init
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO cofx to read defaults from user's config/cookie
;; ex: default-page
(rf/reg-event-db
  ::init-db
  (fn [_] (db/initial-db)))

(rf/reg-event-fx
  ::set-page
  (fn [{:keys [db]} [_ page]]
    {:db (assoc db :current-page page)}))

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
