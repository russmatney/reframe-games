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
  (fn [_] db/initial-db))

(rf/reg-event-fx
  ::select-game
  (fn [{:keys [db]} [_ game]]
    {:dispatch (case game
                 :tetris [::tetris.events/start-game]
                 :puyo   [::puyo.events/start-game])
     :db       (assoc db :selected-game game)}))
