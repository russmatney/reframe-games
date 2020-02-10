(ns games.puyo.events
  (:require
   [re-frame.core :as rf]
   [re-frame.interceptor :as rfi]
   [re-frame.utils :as rfu]
   [games.puyo.db :as puyo.db]
   [games.puyo.core :as puyo]
   [games.controls.events :as controls.events]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Intereceptors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn game-db-interceptor
  "Interceptor that operates over the game db.
  Events passed through this interceptor will
  have their first argument consumed and used to
  find the game-db passed into the handler.

  The event will also be trimmed. Whether this helps
  or causes more insanity remains an open question."
  [game-key]
  (rfi/->interceptor
    :id :game-db-interceptor
    :before (fn game-db-interceptor-before
              [context]
              (let [name (nth (get-in context [:coeffects :event]) 1)]
                (-> context
                    ;; set db to the game's db
                    (assoc-in
                      [:coeffects :db]
                      (get-in context [:coeffects :db game-key name]))

                    ;; set original-db for :after clause
                    (assoc-in
                      [:coeffects ::original-db]
                      (get-in context [:coeffects :db]))

                    ;; trim event object
                    (update-in [:coeffects :event]
                               (fn [event]
                                 (let [trimmed (subvec event 2)]
                                   (if (= 1 (count trimmed))
                                     (first trimmed)
                                     trimmed))))

                    ;; store untrimmed for retrieval
                    (assoc-in [:coeffects ::untrimmed-event]
                              (get-in context [:coeffects :event])))))
    :after (fn game-db-interceptor-after
             [context]
             (let [name       (nth (get-in context [:coeffects ::untrimmed-event]) 1)
                   game-db    (-> context :effects :db)
                   og-db      (-> context :coeffects ::original-db)
                   updated-db (if game-db
                                (assoc-in og-db [game-key name] game-db)
                                og-db)]
               (-> context
                   ;; clean up trimming, retore event
                   (rfu/dissoc-in [:coeffects ::untrimmed-event])
                   (assoc-in [:coeffects :event]
                             (get-in context [:coeffects ::untrimmed-event]))
                   ;; remove our helper
                   (rfu/dissoc-in [:coeffects ::original-db])
                   ;; set the new db on 'EFFECTS' (NOT COEFFECTS)
                   (assoc-in [:effects :db] updated-db))))))


(rf/reg-event-fx
  ::interceptor-example
  [(game-db-interceptor ::puyo.db/db)]
  (fn [{:keys [db]} event]
    (println event)
    (println (keys db))
    {:db db}))

(comment
  (rf/dispatch [::interceptor-example :default "other" "nother"])
  (rf/dispatch [::interceptor-example :default "other"])
  (rf/dispatch [::interceptor-example :default]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game loop
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::start-game
  (fn [{:keys [db]} [_ {:keys [name] :as game-opts}]]
    {:db         (assoc-in db [::puyo.db/db name]
                           (puyo.db/initial-db game-opts))
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
  (fn [{:keys [db]} [_  {:keys [name] :as game-opts}]]
    (let [{:keys [gameover?] :as puyo-db} (-> db ::puyo.db/db (get name))
          puyo-db                         (puyo/step puyo-db game-opts)

          {:keys [tick-timeout] :as puyo-db}
          (if (should-advance-level? puyo-db)
            (advance-level puyo-db)
            puyo-db)]

      ;; TODO consider a gameover, score, etc event model instead
      (if gameover?
        {:clear-timeouts [{:id ::tick}
                          {:id ::game-timer}]}
        {:db      (assoc-in db [::puyo.db/db name] puyo-db)
         :timeout {:id    ::tick
                   :event [::game-tick game-opts]
                   :time  tick-timeout}}))))

(rf/reg-event-fx
  ::game-timer
  (fn [{:keys [db]} [_ {:keys [name] :as game-opts}]]
    (let [{:keys [timer-inc]} (-> db ::puyo.db/db (get name))]
      {:db (update-in db [::puyo.db/db name :time] #(+ % timer-inc))
       :timeout
       ;; TODO update id to use game name
       {:id    ::game-timer
        :event [::game-timer game-opts]
        :time  timer-inc}})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Set Controls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO clean up ignore-controls
(rf/reg-event-fx
  ::set-controls
  (fn [{:keys [db]} [_ {:keys [name ignore-controls]}]]
    (when-not ignore-controls
      {:dispatch
       [::controls.events/set
        (-> db ::puyo.db/db (get name) :controls)]})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Move/Rotate piece
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn can-player-move?
  "Returns true if the game should accept player movement input."
  [{:keys [paused? fall-lock]}]
  (and
    (not paused?)
    (not fall-lock)))

(rf/reg-event-db
  ::move-piece
  (fn [db [_ name direction]]
    (if (can-player-move? (-> db ::puyo.db/db (get name)))
      (update-in db [::puyo.db/db name] #(puyo/move-piece % direction))
      db)))

(rf/reg-event-db
  ::rotate-piece
  (fn [db [_ name]]
    (if (can-player-move? (-> db ::puyo.db/db (get name)))
      (update-in db [::puyo.db/db name] #(puyo/rotate-piece %))
      db)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hold/Swap
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO dry up hold/swap logic?
(rf/reg-event-db
  ::hold-and-swap-piece
  (fn [db [_ name]]
    ;; if there is a hold, move current hold to front of queue
    ;; remove current falling piece from board, move it to hold
    (let [puyo-db       (-> db ::puyo.db/db (get name))
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
              (assoc :hold-lock true)

              ;; TODO handle this better!
              ;; decrement current-piece-num (it's about be re-inc in add-new-piece)
              ;; NOTE that current-piece-num excludes holds to supports combos
              falling-shape
              (update :current-piece-num dec)))]

      (assoc-in db [::puyo.db/db name] puyo-db))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pause
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO dry up pause logic?
;; pauses, ignoring whatever the current state is
(rf/reg-event-fx
  ::pause-game
  (fn [{:keys [db]} [_ {:keys [name]}]]
    (let [updated-db (assoc-in db [::puyo.db/db name :paused?] true)]
      {:db             updated-db
       :clear-timeouts [{:id ::tick}
                        {:id ::game-timer}]})))

;; resumes the game
(rf/reg-event-fx
  ::resume-game
  (fn [{:keys [db]} [_ {:keys [name] :as game-opts}]]
    (let [updated-db (assoc-in db [::puyo.db/db name :paused?] false)]
      {:db         updated-db
       :dispatch-n [[::game-tick game-opts]
                    [::game-timer game-opts]]})))

(rf/reg-event-fx
  ::toggle-pause
  (fn [{:keys [db]} [_ {:keys [name] :as game-opts}]]
    (let [paused (-> db ::puyo.db/db (get name) :paused?)]
      (if-not (-> db ::puyo.db/db (get name) :gameover?)
        (if paused
          ;; unpause
          {:dispatch [::resume-game game-opts]}
          ;; pause
          {:dispatch [::pause-game game-opts]})))))
