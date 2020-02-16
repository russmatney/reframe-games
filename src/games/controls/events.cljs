(ns games.controls.events
  (:require
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [games.controls.re-pressed :as controls.rp]
   [games.events.interceptors :refer [game-db-interceptor]]
   [games.controls.core :as controls]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Init controls listener, global controls, and controls game
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::init
  (fn [_cofx]
    {:dispatch-n
     [[::rp/add-keyboard-event-listener "keydown"]
      [::controls.rp/register-key-listeners]
      [::controls.rp/register-key-dispatchers]]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public event to set controls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::set
  [rf/trim-v]
  (fn [{:keys [db]} [controls]]
    ;; currently merges controls into db
    ;; might want to keep globals only...
    ;; maybe preserve globals with a :global? flag ?
    (let [controls (concat (:controls db) (or controls []))
          by-key   (controls.rp/controls->by-key controls)]

      {:db (assoc db
                  :controls controls
                  :controls-by-key by-key)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Start-games
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO implies games have page knowledge (`pages`)
;; maybe the pages should call out their named-games?
(defn for-page?
  [page {:keys [game-opts] :as _game-db}]
  (contains? (:pages game-opts) page))

;; TODO mark games :started and use to add controls?
(rf/reg-event-fx
  ::start-games
  (fn [{:keys [db]}]
    (let [page (:current-page db)
          game-opts-for-page
          (-> db
              :controls-games
              (vals)
              (->>
                (filter #(for-page? page %))
                (map :game-opts)))]
      {:dispatch-n
       (map (fn [gopts] [::start-game gopts]) game-opts-for-page)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls 'game'
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::start-game
  [(game-db-interceptor :controls-games)]
  (fn [{:keys [_db]} game-opts]
    {:dispatch [::set-controls game-opts]}))

(rf/reg-event-fx
  ::set-controls
  [(game-db-interceptor :controls-games)]
  (fn [{:keys [db]} {:keys [ignore-controls]}]
    (when-not ignore-controls
      {:dispatch [::set (:controls db)]})))

(rf/reg-event-db
  ::add-piece
  [(game-db-interceptor :controls-games)]
  (fn [db _game-opts]
    (controls/add-pieces db)))

(rf/reg-event-db
  ::move-piece
  [(game-db-interceptor :controls-games)]
  (fn [db [_game-opts direction]]
    (if (controls/move-allowed? db)
      (controls/move-piece db direction)
      db)))

(rf/reg-event-db
  ::instant-down
  [(game-db-interceptor :controls-games)]
  (fn [db _game-opts]
    (if (controls/move-allowed? db)
      (controls/instant-down db)
      db)))

(rf/reg-event-db
  ::rotate-piece
  [(game-db-interceptor :controls-games)]
  (fn [db _game-opts]
    (if (controls/move-allowed? db)
      (controls/rotate-piece db)
      db)))

(rf/reg-event-db
  ::toggle-debug
  [(game-db-interceptor :controls-games)]
  (fn [db _game-opts]
    (update-in db [:game-opts :debug?] not)))
