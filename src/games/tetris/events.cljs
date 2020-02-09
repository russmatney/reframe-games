(ns games.tetris.events
  (:require
   [re-frame.core :as rf]
   [games.tetris.db :as tetris.db]
   [games.tetris.core :as tetris]
   [games.controls.events :as controls.events]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game loop
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::start-game
  (fn [{:keys [db]} [_ {:keys [name] :as game-opts}]]
    {:db         (assoc-in db [::tetris.db/db name]
                           (tetris.db/initial-db game-opts))
     :dispatch-n [[::set-controls game-opts]
                  [::game-tick game-opts]
                  [::game-timer game-opts]]}))

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
  (fn [{:keys [db]} [_ {:keys [name] :as game-opts}]]
    (let [{:keys [gameover?] :as tetris-db} (-> db ::tetris.db/db (get name))
          tetris-db                         (tetris/step tetris-db game-opts)

          {:keys [tick-timeout] :as tetris-db}
          (if (should-advance-level? tetris-db)
            (advance-level tetris-db)
            tetris-db)]
      (if gameover?
        {:clear-timeouts [{:id ::tick}
                          {:id ::game-timer}]}
        {:db      (assoc-in db [::tetris.db/db name] tetris-db)
         :timeout {:id    ::tick
                   :event [::game-tick game-opts]
                   :time  tick-timeout}}))))

(rf/reg-event-fx
  ::game-timer
  (fn [{:keys [db]}  [_ {:keys [name] :as game-opts}]]
    (let [{:keys [timer-inc]} (-> db ::tetris.db/db (get name))]
      {:db (update-in db [::tetris.db/db name :time] #(+ % timer-inc))
       :timeout
       {:id    ::game-timer
        :event [::game-timer game-opts]
        :time  timer-inc}})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Set Controls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO clean up ignore-controls
(rf/reg-event-fx
  ::set-controls
  (fn [{:keys [db]} [_ {:keys [name ignore-controls] :as game-opts}]]
    (when-not ignore-controls
      (let [controls (-> db ::tetris.db/db (get name) :controls)]
        {:dispatch [::controls.events/set controls]}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Move/Rotate piece
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn can-player-move?
  "Returns true if the game should accept player movement input."
  [{:keys [paused?]}]
  (not paused?))

(rf/reg-event-db
  ::move-piece
  (fn [db [_ name direction]]
    (if (can-player-move? (-> db ::tetris.db/db (get name)))
      (update-in db [::tetris.db/db name] #(tetris/move-piece % direction))
      db)))

(rf/reg-event-db
  ::rotate-piece
  (fn [db [_ name]]
    (if (can-player-move? (-> db ::tetris.db/db (get name)))
      (update-in db [::tetris.db/db name] #(tetris/rotate-piece %))
      db)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hold/Swap
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-db
  ::hold-and-swap-piece
  (fn [db [_ name]]
    ;; if there is a hold, move current hold to front of queue
    ;; remove current falling piece from board, move it to hold
    (let [tet-db        (-> db ::tetris.db/db (get name))
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

      (assoc-in db [::tetris.db/db name] tet-db))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pause
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; pauses, ignoring whatever the current state is
(rf/reg-event-fx
  ::pause-game
  (fn [{:keys [db]} [_ {:keys [name]}]]
    (let [updated-db (assoc-in db [::tetris.db/db name :paused?] true)]
      {:db             updated-db
       :clear-timeouts [{:id ::tick}
                        {:id ::game-timer}]})))

;; resumes the game
(rf/reg-event-fx
  ::resume-game
  (fn [{:keys [db]} [_ {:keys [name] :as game-opts}]]
    (let [updated-db (assoc-in db [::tetris.db/db name :paused?] false)]
      {:db         updated-db
       :dispatch-n [[::game-tick game-opts]
                    [::game-timer game-opts]]})))

(rf/reg-event-fx
  ::toggle-pause
  (fn [{:keys [db]} [_ {:keys [name] :as game-opts}]]
    (let [paused (-> db ::tetris.db/db (get name) :paused?)]
      (if-not (-> db ::tetris.db/db (get name) :gameover?)
        (if paused
          ;; unpause
          {:dispatch [::resume-game game-opts]}
          ;; pause
          {:dispatch [::pause-game game-opts]})))))
