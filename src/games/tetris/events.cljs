(ns games.tetris.events
  (:require
   [re-frame.core :as rf]
   [games.tetris.db :as tetris.db]
   [games.tetris.core :as tetris]
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
          {:db (assoc-in db [::tetris.db/db :current-view] new-view)}

        should-pause?
        (assoc :dispatch [::pause-game])))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game loop
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::start-game
  (fn [{:keys [db]} [_ game-opts]]
    {:db         (assoc db ::tetris.db/db
                        (tetris.db/initial-db game-opts))
     :dispatch-n [[::set-controls]
                  [::game-tick]
                  [::game-timer]]}))

(defn should-advance-level?
  [{:keys [level rows-per-level rows-cleared]}]
  (>= rows-cleared (* level rows-per-level)))

(defn advance-level
  "Each level updates the step timeout to 90% of the current speed."
  [db]
  (-> db
      (update :level inc)
      (update :tick-timeout #(.floor js/Math (* % 0.9)))))

(rf/reg-event-fx
  ::game-tick
  (fn [{:keys [db]}]
    (let [{:keys [gameover?] :as tetris-db} (::tetris.db/db db)
          tetris-db                         (tetris/step tetris-db)

          {:keys [tick-timeout] :as tetris-db}
          (if (should-advance-level? tetris-db)
            (advance-level tetris-db)
            tetris-db)]
      (if gameover?
        {:clear-timeouts [{:id ::tick}
                          {:id ::game-timer}]}
        {:db      (assoc db ::tetris.db/db tetris-db)
         :timeout {:id    ::tick
                   :event [::game-tick]
                   :time  tick-timeout}}))))

(rf/reg-event-fx
  ::game-timer
  (fn [{:keys [db]}]
    (let [{:keys [timer-inc]} (::tetris.db/db db)]
      {:db (update-in db [::tetris.db/db :time] #(+ % timer-inc))
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
    (let [controls (-> db ::tetris.db/db :controls)]
      {:dispatch [::controls.events/set controls]})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Move/Rotate piece
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn can-player-move?
  "Returns true if the game should accept player movement input."
  [{:keys [paused?]}]
  (not paused?))

(rf/reg-event-db
  ::move-piece
  (fn [db [_ direction]]
    (if (can-player-move? (-> db ::tetris.db/db))
      (update db ::tetris.db/db #(tetris/move-piece % direction))
      db)))

(rf/reg-event-db
  ::rotate-piece
  (fn [db _]
    (if (can-player-move? (-> db ::tetris.db/db))
      (update db ::tetris.db/db #(tetris/rotate-piece %))
      db)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hold/Swap
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-db
  ::hold-and-swap-piece
  (fn [db _]
    ;; if there is a hold, move current hold to front of queue
    ;; remove current falling piece from board, move it to hold
    (let [tet-db        (::tetris.db/db db)
          held          (:held-shape-fn tet-db)
          falling-shape (:falling-shape-fn tet-db)
          hold-lock     (:hold-lock tet-db)
          paused?       (:paused? tet-db)
          tet-db
          (if
              ;; No holding if nothing falling, or if hold-lock in effect
              (or (not falling-shape)
                  hold-lock
                  paused?)
            tet-db
            (cond-> tet-db
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
              (tetris/clear-falling-cells)

              ;; update grid for showing held piece
              falling-shape
              (update :held-grid
                      #(tetris/add-preview-piece % falling-shape))

              ;; indicate that a piece was held to prevent double-holds
              falling-shape
              (assoc :hold-lock true)))]


      (assoc db ::tetris.db/db tet-db))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pause
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; pauses, ignoring whatever the current state is
(rf/reg-event-fx
  ::pause-game
  (fn [{:keys [db]} _ _]
    (let [updated-db (assoc-in db [::tetris.db/db :paused?] true)]
      {:db             updated-db
       :clear-timeouts [{:id ::tick}
                        {:id ::game-timer}]})))

;; resumes the game
(rf/reg-event-fx
  ::resume-game
  (fn [{:keys [db]} _ _]
    (let [game-in-view? (= :game (get-in db [::tetris.db/db :current-view]))
          updated-db    (assoc-in db [::tetris.db/db :paused?] false)]
      (when game-in-view?
        {:db         updated-db
         :dispatch-n [[::game-tick]
                      [::game-timer]]}))))

(rf/reg-event-fx
  ::toggle-pause
  (fn [{:keys [db]} _ _]
    (let [paused (-> db ::tetris.db/db :paused?)]
      (if-not (-> db ::tetris.db/db :gameover?)
        (if paused
          ;; unpause
          {:dispatch [::resume-game]}
          ;; pause
          {:dispatch [::pause-game]})))))
