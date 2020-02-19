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

(defn games-for-page
  [db page]
  (let [games (-> db :games vals)]
    (filter #(for-page? page %)
            games)))

(defn games-not-for-page
  [db page]
  (let [games (-> db :games vals)]
    (remove #(for-page? page %)
            games)))

(rf/reg-event-fx
  ::start-games
  (fn [{:keys [db]}]
    (let [page        (:current-page db)
          start-games (games-for-page db page)
          stop-games  (games-not-for-page db page)]
      {:db (assoc db :active-games (map :name start-games))
       :dispatch-n
       (concat
         (map (fn [game]
                [(:init-event-name game) (:game-opts game)])
              start-games)
         (map (fn [game]
                [(:stop-event-name game) (:game-opts game)])
              stop-games))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Navigation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::set-page
  (fn [{:keys [db]} [_ page]]
    {:db       (assoc db :current-page page)
     :dispatch [::start-games]}))
