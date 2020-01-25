(ns games.tetris.events
 (:require
  [re-frame.core :as rf]
  [re-pressed.core :as rp]
  [games.tetris.db :as tetris.db]
  [games.tetris.core :as tetris]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game loop
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
 ::game-tick
 (fn [{:keys [db]}]
   (let [tetris-db (::tetris.db/db db)
         updated-tetris-db (tetris/step tetris-db)]
    {:db (assoc db ::tetris.db/db updated-tetris-db)
     :timeout {:id ::tick
               :event [::game-tick]
               :time 100}})))

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
      [[[:enter-pressed]
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
       [[::rotate-piece]
        [{:keyCode 38}] ;; up arrow
        [{:keyCode 75}]] ;; k key
       [[::toggle-pause]
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
;; Pause
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
 ::pause-game
 (fn [{:keys [db]} _ _]
   {:db db
    :clear-timeout {:id ::tick}}))

(rf/reg-event-fx
 ::toggle-pause
 (fn [{:keys [db]} _ _]
   (let [paused (-> db ::tetris.db/db :paused?)
         updated-db (update-in db [::tetris.db/db :paused?] not)]
     (if paused
       {:dispatch [::game-tick]
        :db updated-db}
       {:dispatch [::pause-game]
        :db updated-db}))))
