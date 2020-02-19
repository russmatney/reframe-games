(ns games.controls.events
  (:require
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [games.controls.re-pressed :as controls.rp]
   [games.events.interceptors :refer [game-db-interceptor]]
   [games.controls.core :as controls]
   [adzerk.cljs-console :as log]))

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

(defn set-controls
  "Takes a list of control objects, and sets them on the db.
  Broken out to support registering and deregistering controls.
  "
  [db controls]
  (let [by-key (controls.rp/controls->by-key controls)]
    (log/info "Registering ~{(count controls)} controls.")
    (assoc db
           :controls controls
           :controls-by-key by-key)))

(rf/reg-event-fx
  ::register
  [rf/trim-v]
  (fn [{:keys [db]} [controls]]
    (let [controls (concat (:controls db) (or controls []))]
      (log/info "Registering ~{(count controls)} controls.")
      {:db (set-controls db controls)})))

(rf/reg-event-fx
  ::deregister
  [rf/trim-v]
  (fn [{:keys [db]} [controls-to-remove]]
    (let [ids-to-remove (set (map :id controls-to-remove))
          controls      (:controls db)
          controls      (remove
                          (fn [{:keys [id]}]
                            (contains? ids-to-remove id))
                          controls)]
      (log/info "Deregistering ~{(count ids-to-remove)} controls.")
      {:db (set-controls db controls)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls 'game'
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::init-game
  [(game-db-interceptor)]
  (fn [{:keys [_db]} game-opts]
    {:dispatch [::register-controls game-opts]}))

(rf/reg-event-fx
  ::register-controls
  [(game-db-interceptor)]
  (fn [{:keys [db]} {:keys [ignore-controls]}]
    (when-not ignore-controls
      {:dispatch [::register (:controls db)]})))

(rf/reg-event-fx
  ::deregister-controls
  [(game-db-interceptor)]
  (fn [{:keys [db]} {:keys [ignore-controls]}]
    (when-not ignore-controls
      {:dispatch [::deregister (:controls db)]})))

(rf/reg-event-db
  ::add-piece
  [(game-db-interceptor)]
  (fn [db _game-opts]
    (controls/add-pieces db)))

(rf/reg-event-db
  ::move-piece
  [(game-db-interceptor)]
  (fn [db [_game-opts direction]]
    (if (controls/move-allowed? db)
      (controls/move-piece db direction)
      db)))

(rf/reg-event-db
  ::instant-fall
  [(game-db-interceptor)]
  (fn [db [_game-opts direction]]
    (if (controls/move-allowed? db)
      (controls/instant-fall db direction)
      db)))

(rf/reg-event-db
  ::rotate-piece
  [(game-db-interceptor)]
  (fn [db _game-opts]
    (if (controls/move-allowed? db)
      (controls/rotate-piece db)
      db)))

(rf/reg-event-db
  ::toggle-debug
  [(game-db-interceptor)]
  (fn [db _game-opts]
    (update-in db [:game-opts :debug?] not)))
