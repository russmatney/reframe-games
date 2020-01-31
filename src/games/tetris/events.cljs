(ns games.tetris.events
 (:require
  [re-frame.core :as rf]
  [re-pressed.core :as rp]
  [games.tetris.db :as tetris.db]
  [games.tetris.core :as tetris]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Current view
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
 ::set-view
 (fn [{:keys [db]} [_ new-view]]
   (let [should-pause? (or (= new-view :controls)
                           (= new-view :about))
         should-unpause? (= new-view :game)
         dispatch (cond
                    should-pause? [::pause-game]
                    should-unpause? [::unpause-game]
                    true [])]
     {:db
      (assoc-in db [::tetris.db/db :current-view] new-view)
      :dispatch dispatch})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game loop
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn should-advance-level?
  [{:keys [level pieces-per-level pieces-played]}]
  (>= pieces-played (* level pieces-per-level)))

(defn advance-level
  "Each level updates the step timeout to 90% of the current speed."
  [db]
  (-> db
    (update :level inc)
    (update :tick-timeout #(.floor js/Math (* % 0.9)))))

(rf/reg-event-fx
 ::game-tick
 (fn [{:keys [db]}]
   (let [tetris-db (::tetris.db/db db)
         tetris-db (tetris/step tetris-db)

         {:keys [tick-timeout] :as tetris-db}
         (if (should-advance-level? tetris-db)
            (advance-level tetris-db)
            tetris-db)]
    {:db (assoc db ::tetris.db/db tetris-db)
     :timeout {:id ::tick
               :event [::game-tick]
               :time tick-timeout}})))

(rf/reg-event-fx
  ::inc-game-timer
  (fn [{:keys [db]}]
    (let [{:keys [timer-inc]} (::tetris.db/db db)]
      {:db (update-in db [::tetris.db/db :time] #(+ % timer-inc))
       :timeout
       {:id ::game-timer
        :event [::inc-game-timer]
        :time timer-inc}})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Set Controls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def key-label->re-pressed-key
  {"<enter>" {:keyCode 13}
   "<space>" {:keyCode 32}
   "<left>" {:keyCode 37}
   "<right>" {:keyCode 39}
   "<up>" {:keyCode 38}
   "<down>" {:keyCode 40}
   "h" {:keyCode 72}
   "j" {:keyCode 74}
   "k" {:keyCode 75}
   "l" {:keyCode 76}})

(def control->event
  {:move-left [::move-piece :left]
   :move-right [::move-piece :right]
   :move-down [::move-piece :down]
   :hold-swap [::rotate-piece]
   :pause [::toggle-pause]
   :rotate [::rotate-piece]})

(defn controls->event-keys
  [controls]
  (into []
        (map
         (fn [[control keys]]
           (into []
            (cons
             (control->event control)
             (map (fn [k] [(key-label->re-pressed-key k)]) keys))))
         controls)))

(defn controls->all-keys
  [controls]
  (into []
        (mapcat
         (fn [[_ keys]]
           (map key-label->re-pressed-key keys))
         controls)))

(rf/reg-event-fx
 ::set-controls
 (fn [{:keys [db]}]
   (let [controls (-> db ::tetris.db/db :controls)
         event-keys (controls->event-keys controls)
         prevent-default-keys (controls->all-keys controls)]
     {:db db
      :dispatch
      [::rp/set-keydown-rules
       {:event-keys event-keys
        :prevent-default-keys prevent-default-keys}]})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Move piece
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
 ::move-piece
 (fn [{:keys [db]} [_ direction]]
   (let [paused? (-> db ::tetris.db/db :paused?)]
     {:db
      (if paused? db
        (update db
                ::tetris.db/db
                (fn [t-db]
                  (tetris/move-piece t-db direction))))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Rotate piece
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
 ::rotate-piece
 (fn [{:keys [db]} _ _]
   (let [paused? (-> db ::tetris.db/db :paused?)]
     {:db
      (if paused? db
        (update db
                ::tetris.db/db
                (fn [t-db]
                  (tetris/rotate-piece t-db))))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hold/Swap
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
 ::hold-and-swap-piece
 (fn [{:keys [db]} _ _]
   ;; if there is a hold, move current hold to front of queue
   ;; remove current falling piece from board, move it to hold
   (let [tet-db (::tetris.db/db db)
         held (:held-shape-fn tet-db)
         falling-shape (:falling-shape-fn tet-db)
         hold-lock (:hold-lock tet-db)
         paused? (:paused? tet-db)
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


     {:db (assoc db ::tetris.db/db tet-db)})))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pause
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; pauses, ignoring whatever the current state is
(rf/reg-event-fx
 ::pause-game
 (fn [{:keys [db]} _ _]
   (let [updated-db (assoc-in db [::tetris.db/db :paused?] true)]
     {:db updated-db
      :clear-timeouts [{:id ::tick}
                       {:id ::game-timer}]})))

;; resumes the game
(rf/reg-event-fx
 ::resume-game
 (fn [{:keys [db]} _ _]
   (let [updated-db (assoc-in db [::tetris.db/db :paused?] false)]
     {:db updated-db
      :dispatch-n [[::game-tick]
                   [::inc-game-timer]]})))

(rf/reg-event-fx
 ::toggle-pause
 (fn [{:keys [db]} _ _]
   (let [paused (-> db ::tetris.db/db :paused?)]
     (if paused
       ;; unpause
       {:dispatch [::resume-game]}
       ;; pause
       {:dispatch [::pause-game]}))))
