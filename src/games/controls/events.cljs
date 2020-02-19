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
;; Global control registry
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn distinct-by [f coll]
  (let [groups (group-by f coll)]
    (map #(first (groups %)) (distinct (map f coll)))))

(defn set-controls
  "Takes a list of control objects, and sets them on the db.
  Broken out to support registering and deregistering controls.
  "
  [db controls]
  (let [by-key (controls.rp/controls->by-key controls)]
    (assoc db
           :controls controls
           :controls-by-key by-key)))

(rf/reg-event-fx
  ::register
  [rf/trim-v]
  (fn [{:keys [db]} [controls]]
    (let [controls (concat (:controls db) (or controls []))
          controls (distinct-by :id controls)]
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
      {:db (set-controls db controls)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game control events
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::register-controls
  [(game-db-interceptor)]
  (fn [{:keys [db]} {:keys [ignore-controls]}]
    (when-not ignore-controls
      {:dispatch [::register (:controls db)]})))

(rf/reg-event-fx
  ::deregister-controls
  [(game-db-interceptor)]
  (fn [{:keys [db]} _game-opts]
    {:dispatch [::deregister (:controls db)]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls 'game'
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::init-game
  [(game-db-interceptor)]
  (fn [{:keys [_db]} game-opts]
    {:dispatch [::register-controls game-opts]}))

(rf/reg-event-fx
  ::stop-game
  [(game-db-interceptor)]
  (fn [{:keys [_db]} game-opts]
    {:dispatch [::deregister-controls game-opts]}))

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
