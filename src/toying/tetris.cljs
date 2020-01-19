(ns toying.tetris
 (:require
  [re-frame.core :as rf]
  [toying.events :as evts]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game constants

(def grid-height 12)
(def grid-width 5)

(def initial-game-state
  {:grid
   (vec (take grid-height (repeat (vec (take grid-width (repeat {}))))))
   :phase :falling})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game functions

(defn add-new-piece [db]
  (let [grid (-> db :game-state :grid)
        updated-grid
        (update-in grid [0 2]
          (fn [cell]
            (assoc cell :falling true)))
        updated-db
        (assoc-in db [:game-state :grid] updated-grid)]
    updated-db))

(defn step-falling [db]
  (let [grid (-> db :game-state :grid)
        all-cells (flatten grid)
        falling-piece? (seq (filter :falling all-cells))]
    (println falling-piece?)
    (cond
      ;; TODO lose condition if a new piece can't be added
      (not falling-piece?)
      (add-new-piece db)

      true db)))

(defn step [db]
  (let [phase (:game-phase db)]
    (case phase
      :falling (step-falling db)
      nil (step-falling db))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DB

(def initial-db
  {:game-state initial-game-state})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Events

(rf/reg-event-db
 ::init-db
 (fn [db]
   (assoc db ::tetris initial-db)))

;; this is not quite right, but meh for now
(rf/dispatch-sync [::init-db])

(rf/reg-event-fx
 ::game-tick
 (fn [{:keys [db]}]
   (let [tetris-db (::tetris db)
         updated-tetris-db (step tetris-db)]
     (println updated-tetris-db)
    {:db (assoc db ::tetris updated-tetris-db)
     :timeout {:id ::tick
               :event [::game-tick]
               :time 1000}})))

(rf/dispatch-sync [::game-tick])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Subs

(rf/reg-sub
 ::tetris-db
 (fn [db]
   (::tetris db)))

(rf/reg-sub
 ::game-state
 :<- [::tetris-db]
 (fn [db]
   (:game-state db)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(defn cell [{:keys [falling]}]
 ^{:key (str (random-uuid))}
 [:div
  {:style
   {:max-width "30px"
    :max-height "30px"
    :width "30px"
    :height "30px"
    :background (if falling "coral" "powderblue")
    :border "black solid 1px"}}
  ""])

(defn stage []
  (let [game-state @(rf/subscribe [::game-state])]
   [:div
    (for [row (:grid game-state)]
      ^{:key (str (random-uuid))}
      [:div
       {:style {:display "flex"}}
       (for [cell-state row]
        (cell cell-state))])]))

