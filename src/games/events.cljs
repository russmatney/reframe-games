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
  ::select-game
  (fn [{:keys [db]} [_ game]]
    (cond-> {:db (assoc db :selected-game game)}
      game
      (assoc :dispatch
             (case game
               ;; TODO consider conditional resume-game,
               ;; or start-or-resume event
               :tetris [::tetris.events/start-game {:name :default}]
               :puyo   [::puyo.events/start-game {:name :default}])))))

(rf/reg-event-fx
  ::deselect-game
  (fn [{:keys [db]} _]
    (let [current-game (-> db :selected-game)]
      (cond-> {:db (dissoc db :selected-game)}
        current-game
        (assoc :dispatch
               (case current-game
                 :tetris [::tetris.events/pause-game]
                 :puyo   [::puyo.events/pause-game]))))))
