(ns games.puyo.events
  (:require
   [re-frame.core :as rf]
   [games.puyo.db :as puyo.db]
   [games.puyo.core :as puyo]
   [games.controls.events :as controls.events]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Current view
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::set-view
  (fn [{:keys [db]} [_ new-view]]
    (let [should-pause? (or (= new-view :controls)
                            (= new-view :about))]
      (cond->
          {:db (assoc-in db [::puyo.db/db :current-view] new-view)}

        should-pause?
        (assoc :dispatch [::pause-game])))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game loop
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::start-game
  (fn [{:keys [db]} _]
    {:db         (assoc db ::puyo.db/db puyo.db/initial-db)
     :dispatch-n [[::set-controls]
                  [::game-tick]
                  [::game-timer]]}))

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
  (fn [{:keys [db]}]
    (let [{:keys [gameover?] :as puyo-db} (::puyo.db/db db)
          puyo-db                         (puyo/step puyo-db)

          {:keys [tick-timeout] :as puyo-db}
          (if (should-advance-level? puyo-db)
            (advance-level puyo-db)
            puyo-db)]

      ;; TODO consider a gameover event model instead
      (if gameover?
        {:clear-timeouts [{:id ::tick}
                          {:id ::game-timer}]}
        {:db      (assoc db ::puyo.db/db puyo-db)
         :timeout {:id    ::tick
                   :event [::game-tick]
                   :time  tick-timeout}}))))

(rf/reg-event-fx
  ::game-timer
  (fn [{:keys [db]}]
    (let [{:keys [timer-inc]} (::puyo.db/db db)]
      {:db (update-in db [::puyo.db/db :time] #(+ % timer-inc))
       :timeout
       {:id    ::game-timer
        :event [::game-timer]
        :time  timer-inc}})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Set Controls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::set-controls
  (fn [{:keys [db]}]
    {:dispatch [::controls.events/set (-> db ::puyo.db/db :controls)]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Move/Rotate piece
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO dry up move logic?
(rf/reg-event-fx
  ::move-piece
  (fn [{:keys [db]} [_ direction]]
    (let [paused? (-> db ::puyo.db/db :paused?)]
      {:db
       (if paused? db
           (update db
                   ::puyo.db/db
                   (fn [t-db]
                     (puyo/move-piece t-db direction))))})))


;; TODO dry up rotate logic?
(rf/reg-event-fx
  ::rotate-piece
  (fn [{:keys [db]} _ _]
    (let [paused? (-> db ::puyo.db/db :paused?)]
      {:db
       (if paused? db
           (update db
                   ::puyo.db/db
                   (fn [t-db]
                     (puyo/rotate-piece t-db))))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hold/Swap
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO dry up hold/swap logic?
(rf/reg-event-fx
  ::hold-and-swap-piece
  (fn [{:keys [db]} _ _]
    ;; if there is a hold, move current hold to front of queue
    ;; remove current falling piece from board, move it to hold
    (let [puyo-db       (::puyo.db/db db)
          held          (:held-shape-fn puyo-db)
          falling-shape (:falling-shape-fn puyo-db)
          hold-lock     (:hold-lock puyo-db)
          paused?       (:paused? puyo-db)
          puyo-db
          (if
              ;; No holding if nothing falling, or if hold-lock in effect
              (or (not falling-shape)
                  hold-lock
                  paused?)
            puyo-db
            (cond-> puyo-db
              ;; prepend queue with held piece
              ;; TODO prevent quick double tap from stacking the queue here
              held
              (update :piece-queue (fn [q]
                                     (cons held q)))

              ;; move falling piece to held piece
              falling-shape
              (assoc :held-shape-fn falling-shape)

              ;; clear falling piece if there was one
              falling-shape
              (assoc :falling-shape-fn nil)

              ;; clear the falling pieces from the board
              falling-shape
              (puyo/clear-falling-cells)

              ;; update grid for showing held piece
              falling-shape
              (update :held-grid
                      #(puyo/add-preview-piece % falling-shape))

              ;; indicate that a piece was held to prevent double-holds
              falling-shape
              (assoc :hold-lock true)))]

      {:db (assoc db ::puyo.db/db puyo-db)})))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pause
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO dry up pause logic?
;; pauses, ignoring whatever the current state is
(rf/reg-event-fx
  ::pause-game
  (fn [{:keys [db]} _ _]
    (let [updated-db (assoc-in db [::puyo.db/db :paused?] true)]
      {:db             updated-db
       :clear-timeouts [{:id ::tick}
                        {:id ::game-timer}]})))

;; resumes the game
(rf/reg-event-fx
  ::resume-game
  (fn [{:keys [db]} _ _]
    (let [game-in-view? true ;;(= :game (get-in db [::puyo.db/db :current-view]))
          updated-db    (assoc-in db [::puyo.db/db :paused?] false)]
      (when game-in-view?
        {:db         updated-db
         :dispatch-n [[::game-tick]
                      [::game-timer]]}))))

(rf/reg-event-fx
  ::toggle-pause
  (fn [{:keys [db]} _ _]
    (let [paused (-> db ::puyo.db/db :paused?)]
      (if-not (-> db ::puyo.db/db :gameover?)
        (if paused
          ;; unpause
          {:dispatch [::resume-game]}
          ;; pause
          {:dispatch [::pause-game]})))))
