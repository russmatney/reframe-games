(ns games.events
  (:require
   [re-frame.core :as rf]
   [games.tetris.events :as tetris.events]
   [games.puyo.events :as puyo.events]
   [games.db :as db]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Init
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-db
  ::init-db
  (fn [_] (db/initial-db)))

(rf/reg-event-fx
  ::set-view
  (fn [{:keys [db]} [_ view]]
    {:db (assoc db :current-view view)}))

(rf/reg-event-fx
  ::unset-view
  (fn [{:keys [db]} _]
    (let [current-view (-> db :current-view)]
      (cond-> {:db (dissoc db :current-view)}
        (contains? #{:tetris :puyo} current-view)
        (assoc :dispatch
               (case current-view
                 :tetris [::tetris.events/pause-game]
                 :puyo   [::puyo.events/pause-game]))))))
