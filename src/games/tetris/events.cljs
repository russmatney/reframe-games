(ns games.tetris.events
 (:require
  [re-frame.core :as rf]
  [re-pressed.core :as rp]
  [games.tetris.db :as tetris.db]
  [games.tetris.core :as tetris]))

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

(rf/reg-event-fx
 ::set-controls
 (fn [{:keys [db]}]
   {:db db
    :dispatch
    [::rp/set-keydown-rules
     {;; takes a collection of events followed by key combos that can trigger the
      ;; event
      :event-keys
      [[[::toggle-pause]
        [{:keyCode 13}]] ;; enter key
       [[::move-piece :left]
        [{:keyCode 37}] ;; left arrow
        [{:keyCode 72}]] ;; h key
       [[::move-piece :right]
        [{:keyCode 39}] ;;right arrow
        [{:keyCode 76}]] ;; l key
       [[::move-piece :down]
        [{:keyCode 40}] ;; down arrow
        [{:keyCode 74}]] ;; j key
       [[::hold-and-swap-piece]
        [{:keyCode 38}] ;; up arrow
        [{:keyCode 75}]] ;; k key
       [[::rotate-piece]
        [{:keyCode 32}]]]}]})) ;; space bar

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Move piece
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
 ::move-piece
 (fn [{:keys [db]} [_ direction]]
   {:db
    (update db
            ::tetris.db/db
            (fn [t-db]
              (tetris/move-piece t-db direction)))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Rotate piece
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
 ::rotate-piece
 (fn [{:keys [db]} _ _]
   {:db
    (update db
            ::tetris.db/db
            (fn [t-db]
              (tetris/rotate-piece t-db)))}))

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
         tet-db
         (if
           ;; No holding if nothing falling, or if hold-lock in effect
           (or (not falling-shape)
               hold-lock)
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

(rf/reg-event-fx
 ::pause-game
 (fn [{:keys [db]} _ _]
   {:db db
    :clear-timeouts [{:id ::tick}
                     {:id ::game-timer}]}))

(rf/reg-event-fx
 ::toggle-pause
 (fn [{:keys [db]} _ _]
   (let [paused (-> db ::tetris.db/db :paused?)
         updated-db (update-in db [::tetris.db/db :paused?] not)]
     (if paused
       ;; unpause
       {:dispatch-n [[::game-tick]
                     [::inc-game-timer]]
        :db updated-db}
       ;; pause
       {:dispatch [::pause-game]
        :db updated-db}))))
