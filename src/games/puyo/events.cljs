(ns games.puyo.events
  (:require
   [re-frame.core :as rf]
   [games.events.interceptors :refer [game-db-interceptor]]
   [games.puyo.db :as puyo.db]
   [games.puyo.core :as puyo]
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
                  [::game-tick game-opts]
                  [::game-timer game-opts]
                  ]}))


(defn should-advance-level?
  [{:keys [level groups-per-level groups-cleared]}]
  (>= groups-cleared (* level groups-per-level)))

(defn advance-level
  "Each level updates the step timeout to 90% of the current speed."
  [db]
  (-> db
      (update :level inc)
      (update :tick-timeout #(.floor js/Math (* % 0.9)))))

(rf/reg-event-fx
  ::game-tick
  [(game-db-interceptor ::puyo.db/db)]
  (fn [{:keys [db]} game-opts]
    (let [db (puyo/step db game-opts)

          {:keys [tick-timeout] :as db}
          (if (should-advance-level? db)
            (advance-level db)
            db)]

      ;; TODO consider a gameover, score, piece-played etc event model
      (if (:gameover? db)
        {:clear-timeouts [{:id ::tick}
                          {:id ::game-timer}]}
        {:db      db
         :timeout {:id    ::tick
                   :event [::game-tick game-opts]
                   :time  tick-timeout}}))))

(rf/reg-event-fx
  ::game-timer
  [(game-db-interceptor ::puyo.db/db)]
  (fn [{:keys [db]} game-opts]
    (let [{:keys [timer-inc]} db]
      {:db (update db :time #(+ % timer-inc))
       :timeout
       ;; TODO update id to use game name
       {:id    ::game-timer
        :event [::game-timer game-opts]
        :time  timer-inc}})))

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

;; (games.pause/reg-events
;;   {:registry       ::puyo.db/db
;;    :clear-timeouts [{:id ::tick}
;;                     {:id ::game-timer}]
;;    :on-resume      {:dispatch-n [[::game-tick game-opts]
;;                                  [::game-timer game-opts]]}})


;; TODO dry up pause logic?
;; pauses, ignoring whatever the current state is
(rf/reg-event-fx
  ::pause-game
  [(game-db-interceptor ::puyo.db/db)]
  (fn [{:keys [db]} _game-opts]
    {:db             (assoc db :paused? true)
     :clear-timeouts [{:id ::tick}
                      {:id ::game-timer}]}))

;; resumes the game
(rf/reg-event-fx
  ::resume-game
  [(game-db-interceptor ::puyo.db/db)]
  (fn [{:keys [db]} game-opts]
    (let [updated-db (assoc db :paused? false)]
      {:db         updated-db
       :dispatch-n [[::game-tick game-opts]
                    [::game-timer game-opts]]})))

(rf/reg-event-fx
  ::toggle-pause
  [(game-db-interceptor ::puyo.db/db)]
  ;; NOTE that events coming from keybds have extra event args,
  ;; so the interceptor passes it as a list rather than game-opts directly
  (fn [{:keys [db]} [game-opts]]
    (if-not (:gameover? db)
      (if (:paused? db)
        ;; unpause
        {:dispatch [::resume-game game-opts]}
        ;; pause
        {:dispatch [::pause-game game-opts]}))))
