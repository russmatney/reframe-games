(ns games.events
  (:require
   [re-frame.core :as rf]
   [games.tetris.events :as tetris.events]
   [games.puyo.events :as puyo.events]
   [games.controls.events :as controls.events]
   [games.db :as db]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Init
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::init
  (fn [_]
    {:db (db/initial-db)
     :dispatch-n
     [[::start-games]
      [::controls.events/init]]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Start Games
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; implies games have page knowledge (`pages`)
;; maybe the pages should call out their named-games?
(defn for-page?
  [page {:keys [game-opts] :as _game-db}]
  (contains? (:pages game-opts) page))

(rf/reg-event-fx
  ::start-games
  (fn [{:keys [db]}]
    (let [page  (:current-page db)
          games (filter #(for-page? page %)
                        (-> db :games vals))]
      {:db (assoc db :active-games (map :name games))
       :dispatch-n
       (map (fn [game] [(:init-event-name game) (:game-opts game)]) games)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Navigation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::set-page
  (fn [{:keys [db]} [_ page]]
    {:db       (assoc db :current-page page)
     :dispatch [::start-games]}))

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
