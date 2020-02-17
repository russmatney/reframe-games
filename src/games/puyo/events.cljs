(ns games.puyo.events
  (:require
   [re-frame.core :as rf]
   [games.events.interceptors :refer [game-db-interceptor]]
   [games.puyo.db :as puyo.db]
   [games.puyo.core :as puyo]
   [games.pause.core :as pause]
   [games.controls.events :as controls.events]
   [games.puyo.shapes :as puyo.shapes]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Start-games
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO dry this up
;; implies games have page knowledge (`pages`)
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
              ::puyo.db/db ;; TODO rename games-map key?
              (vals)
              (->>
                (filter #(for-page? page %))
                (map :game-opts)))]
      {:dispatch-n
       (map (fn [gopts] [::start-game gopts]) game-opts-for-page)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game loop
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::start-game
  [(game-db-interceptor ::puyo.db/db)]
  (fn [_cofx game-opts]
    {:dispatch-n [[::set-controls game-opts]
                  [::step game-opts]
                  [::game-timer game-opts]]}))

(rf/reg-event-fx
  ::restart-game
  [(game-db-interceptor ::puyo.db/db)]
  (fn [_cofx game-opts]
    {:db         (puyo.db/game-dbs-map (:name game-opts))
     ;; TODO clean up existing games/controls
     :dispatch-n [[::step game-opts]
                  [::game-timer game-opts]]}))

(rf/reg-event-fx
  ::step
  [(game-db-interceptor ::puyo.db/db)]
  (fn [{:keys [db]} game-opts]
    (let [{:keys [step-timeout] :as db} (puyo/step db game-opts)]
      (if (:gameover? db)
        {:db             db
         :clear-timeouts [{:id ::step}
                          {:id ::game-timer}]}
        {:db      db
         :timeout {:id    ::step
                   :event [::step game-opts]
                   :time  step-timeout}}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Set Controls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO clean up ignore-controls and general controls usage
(rf/reg-event-fx
  ::set-controls
  [(game-db-interceptor ::puyo.db/db)]
  (fn [{:keys [db]} {:keys [ignore-controls]}]
    (when-not ignore-controls
      {:dispatch
       [::controls.events/set (:controls db)]})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Move/Rotate piece
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO update interceptor to handle this (pass game-opts through kbd events)
(rf/reg-event-db
  ::move-piece
  [(game-db-interceptor ::puyo.db/db)]
  (fn [db [_game-opts direction]]
    (if (puyo/can-player-move? db)
      (puyo/move-piece db direction)
      db)))

(rf/reg-event-db
  ::rotate-piece
  [(game-db-interceptor ::puyo.db/db)]
  (fn [db _game-opts]
    (if (puyo/can-player-move? db)
      (puyo/rotate-piece db)
      db)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hold/Swap
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO dry up hold/swap logic?
(rf/reg-event-db
  ::hold-and-swap-piece
  [(game-db-interceptor ::puyo.db/db)]
  (fn [db _game-opts]
    (let [{:keys [held-shape falling-shape hold-lock paused?]} db]
      (if (or (not falling-shape)
              hold-lock
              paused?)
        db
        (cond-> db
          ;; prepend queue with held piece
          held-shape
          (update :piece-queue (fn [q]
                                 (cons held-shape q)))

          falling-shape
          (->
            ;; move falling piece to held piece
            (assoc :held-shape falling-shape)
            ;; clear falling piece if there was one
            (assoc :falling-shape nil)
            ;; clear the falling pieces from the board
            (puyo/clear-falling-cells)
            ;; update grid for showing held piece
            (update :held-grid
                    #(puyo/add-preview-piece
                       % (puyo.shapes/build-piece-fn falling-shape)))

            ;; indicate that a piece was held to prevent double-holds
            (assoc :hold-lock true)

            ;; TODO handle this better!
            ;; decrement current-piece-num (it's about be re-inc in add-new-piece)
            ;; NOTE that current-piece-num excludes holds to supports combos
            (update :current-piece-num dec)))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pause
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(pause/reg-pause-events
  {:n            :games.puyo.events
   :game-map-key ::puyo.db/db
   :timers       [{:game-opts->id    (fn [_] ::step)
                   :game-opts->event (fn [gopts] [::step gopts])}]})
