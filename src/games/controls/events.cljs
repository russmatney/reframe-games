(ns games.controls.events
  (:require
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [games.events.interceptors :refer [game-db-interceptor]]
   [games.controls.core :as controls]
   [games.controls.db :as controls.db]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Init controls listener, global controls, and controls game
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::init
  (fn [_cofx]
    {:dispatch-n
     [[::rp/add-keyboard-event-listener "keydown"]
      [::set]]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public event to set controls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO update to support multiple events from seperate key bindings
;; to support sending keys to multiple keys at once
(rf/reg-event-fx
  ::set
  [rf/trim-v]
  (fn [{:keys [db]} [controls]]
    (let [controls   (merge controls.db/global-controls (or controls {}))
          event-keys (controls/controls->rp-event-keys controls)
          all-keys   (controls/controls->rp-all-keys controls)]
      {:db (assoc db :controls controls)
       :dispatch
       [::rp/set-keydown-rules
        {:event-keys           event-keys
         :prevent-default-keys all-keys}]})))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls 'game'
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::start-game
  [(game-db-interceptor :controls-games)]
  (fn [{:keys [db]} game-opts]
    {:dispatch [::set-controls game-opts]
     :db       (merge (-> (controls.db/initial-db game-opts)
                          (controls/add-piece))
                      ;; prefer whatever is in the db already
                      db)}))

(rf/reg-event-fx
  ::set-controls
  [(game-db-interceptor :controls-games)]
  (fn [{:keys [db]} {:keys [ignore-controls]}]
    (when-not ignore-controls
      {:dispatch [::set (:controls db)]})))

(rf/reg-event-db
  ::add-piece
  [(game-db-interceptor :controls-games)]
  (fn [db [_game-opts]]
    (controls/add-piece db)))

(rf/reg-event-db
  ::move-piece
  [(game-db-interceptor :controls-games)]
  (fn [db [_game-opts direction]]
    (if (controls/can-player-move? db)
      (controls/move-piece db direction)
      db)))

(rf/reg-event-db
  ::rotate-piece
  [(game-db-interceptor :controls-games)]
  (fn [db [_game-opts]]
    (if (controls/can-player-move? db)
      (controls/rotate-piece db)
      db)))

(rf/reg-event-db
  ::toggle-debug
  [(game-db-interceptor :controls-games)]
  (fn [db _game-opts]
    (println "toggling debug")
    (println (:game-opts db))
    (update-in db [:game-opts :debug?] not)))
;; TODO how to merge game-opts?
