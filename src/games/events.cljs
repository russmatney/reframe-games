(ns games.events
 (:require
  [reagent.core :as reagent]
  [re-frame.core :as rf]
  [re-pressed.core :as rp]
  [games.tetris.db :as tetris.db]
  [games.tetris.core :as tetris]
  [games.db :as db]
  [games.events.timeout]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Init
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-db
 ::init-db
 (fn [db] db/initial-db))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game tick
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; TODO move to games.tetris.events

(rf/reg-event-fx
 ::game-tick
 (fn [{:keys [db]}]
   (let [tetris-db (::tetris.db/db db)
         updated-tetris-db (tetris/step tetris-db)]
    {:db (assoc db ::tetris.db/db updated-tetris-db)
     :timeout {:id ::tick
               :event [::game-tick]
               :time 1000}})))

(rf/reg-event-fx
 ::pause-game
 (fn [{:keys [db]}]
   {:db db
    :clear-timeout {:id ::tick}}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/dispatch
 [::rp/set-keydown-rules
  {;; takes a collection of events followed by key combos that can trigger the
   ;; event
   :event-keys [[[:enter-pressed]
                 [{:keyCode 13}]] ;; enter key
                [[:left-pressed]
                 [{:keyCode 37}] ;; left arrow
                 [{:keyCode 72}]] ;; h key
                [[:right-pressed]
                 [{:keyCode 39}] ;;right arrow
                 [{:keyCode 76}]] ;; l key
                [[:down-pressed]
                 [{:keyCode 40}] ;; down arrow
                 [{:keyCode 74}]] ;; j key
                [[:up-pressed]
                 [{:keyCode 38}] ;; up arrow
                 [{:keyCode 75}]] ;; k key
                [[:space-pressed]
                 [{:keyCode 32}]]]}]) ;; space bar

;; TODO customize controls
;; TODO support fast-drop

(rf/reg-event-fx
 :left-pressed
 (fn [{:keys [db]} _ _]
   {:db
    (update db
            ::tetris.db/db
            (fn [t-db]
              (tetris/move-piece t-db :left)))}))

(rf/reg-event-fx
 :right-pressed
 (fn [{:keys [db]} _ _]
   {:db
    (update db
            ::tetris.db/db
            (fn [t-db]
              (tetris/move-piece t-db :right)))}))

(rf/reg-event-fx
 :down-pressed
 (fn [{:keys [db]} _ _]
   {:db
    (update db
            ::tetris.db/db
            (fn [t-db]
              (tetris/move-piece t-db :down)))}))

(rf/reg-event-fx
 :up-pressed
 (fn [{:keys [db]} _ _]
   {:db
    (update db
            ::tetris.db/db
            (fn [t-db]
              (tetris/rotate-piece t-db)))}))

(rf/reg-event-fx
 :space-pressed
 (fn [{:keys [db]} _ _]
   (let [paused (-> db ::tetris.db/db :paused?)
         updated-db (update-in db [::tetris.db/db :paused?] not)]
     (if paused
       {:dispatch [::game-tick]
        :db updated-db}
       {:dispatch [::pause-game]
        :db updated-db}))))
