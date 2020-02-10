(ns games.puyo.events
  (:require
   [re-frame.core :as rf]
   [games.events.interceptors :refer [game-db-interceptor]]
   [games.puyo.db :as puyo.db]
   [games.puyo.core :as puyo]
   [games.pause.core :as pause]
   [games.controls.events :as controls.events]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game loop
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::start-game
  [(game-db-interceptor ::puyo.db/db)]
  (fn [_cofx game-opts]
    {:db         (puyo.db/initial-db game-opts)
     :dispatch-n [[::set-controls game-opts]
                  [::step game-opts]
                  [::pause/game-timer game-opts]
                  ]}))


;; TODO move to core
(defn should-advance-level?
  [{:keys [level groups-per-level groups-cleared]}]
  (>= groups-cleared (* level groups-per-level)))

(defn advance-level
  "Each level updates the step timeout to 90% of the current speed."
  [db]
  (-> db
      (update :level inc)
      (update :step-timeout #(.floor js/Math (* % 0.9)))))

(rf/reg-event-fx
  ::step
  [(game-db-interceptor ::puyo.db/db)]
  (fn [{:keys [db]} game-opts]
    (let [db (puyo/step db game-opts)

          ;; why is this out here vs in core?
          {:keys [step-timeout] :as db}
          (if (should-advance-level? db)
            (advance-level db)
            db)]

      ;; TODO consider a gameover, score, piece-played etc event model
      (if (:gameover? db)
        {:clear-timeouts [{:id ::step}
                          {:id ::pause/game-timer}]}
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

(defn can-player-move?
  "Returns true if the game should accept player movement input."
  [{:keys [paused? fall-lock]}]
  (and
    (not paused?)
    (not fall-lock)))

;; TODO update interceptor to handle this (pass game-opts through kbd events)
(rf/reg-event-db
  ::move-piece
  [(game-db-interceptor ::puyo.db/db)]
  (fn [db [_game-opts direction]]
    (if (can-player-move? db)
      (puyo/move-piece db direction)
      db)))

(rf/reg-event-db
  ::rotate-piece
  [(game-db-interceptor ::puyo.db/db)]
  (fn [db [_game-opts]]
    (if (can-player-move? db)
      (puyo/rotate-piece db)
      db)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hold/Swap
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO dry up hold/swap logic?
(rf/reg-event-db
  ::hold-and-swap-piece
  [(game-db-interceptor ::puyo.db/db)]
  (fn [db [_game-opts]]
    ;; if there is a hold, move current hold to front of piece-queue
    ;; remove current falling piece from board, move it to hold
    (let [{:keys [held-shape-fn falling-shape-fn hold-lock paused?]} db]

      (if ;; if nothing falling or if hold-lock in effect, return db
          (or (not falling-shape-fn)
              hold-lock
              paused?)
        db
        (cond-> db
          ;; prepend queue with held piece
          held-shape-fn
          (update :piece-queue (fn [q]
                                 (cons held-shape-fn q)))

          falling-shape-fn
          (->
            ;; move falling piece to held piece
            (assoc :held-shape-fn falling-shape-fn)
            ;; clear falling piece if there was one
            (assoc :falling-shape-fn nil)
            ;; clear the falling pieces from the board
            (puyo/clear-falling-cells)
            ;; update grid for showing held piece
            (update :held-grid
                    #(puyo/add-preview-piece % falling-shape-fn))

            ;; indicate that a piece was held to prevent double-holds
            (assoc :hold-lock true)

            ;; TODO handle this better!
            ;; decrement current-piece-num (it's about be re-inc in add-new-piece)
            ;; NOTE that current-piece-num excludes holds to supports combos
            (update :current-piece-num dec)))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pause
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; register the pause events
(pause/reg-pause-events
  {:game-map-key ::puyo.db/db
   :timers       [(pause/make-timer ::step)]})
