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
      ;; init global controls
      ;; TODO should this add all controls? controls based on the view?
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
    ;; currently merges controls into db
    ;; might want to keep globals only...
    ;; maybe preserve globals with a :global? flag ?
    (let [controls      (concat (:controls db) (or controls []))
          id-events-map (controls.rp/controls->id-events-map controls)
          event-keys    (controls.rp/controls->rp-event-keys controls)
          all-keys      (controls.rp/controls->rp-all-keys controls)]

      ;; TODO reckless! create a cofx
      (controls.rp/register-dispatchers id-events-map)

      {:db (assoc db :controls controls)
       :dispatch
       [::rp/set-keydown-rules
        {:event-keys           event-keys
         :prevent-default-keys all-keys}]})))

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
    (update-in db [:game-opts :debug?] not)))
