(ns toying.tetris
 (:require
  [re-frame.core :as rf]
  [toying.events :as evts]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game constants

(def grid-height 7)
(def grid-width 5)


(defn set-cell-labels [grid]
  (map-indexed
   (fn [y row]
    (vec
     (map-indexed
      (fn [x cell]
       (assoc cell :y y :x x))
      row)))
   grid))

(def initial-game-state
  {:grid
   (vec
    (set-cell-labels
     (take grid-height (repeat (vec (take grid-width (repeat {})))))))
   :phase :falling})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game functions

(defn add-new-piece [db]
  (let [grid (-> db :game-state :grid)
        updated-grid
        (update-in grid [0 2] ;; TODO un-hardcode this
          (fn [cell]
            (assoc cell :falling true)))
        updated-db
        (assoc-in db [:game-state :grid] updated-grid)]
    updated-db))

(defn get-cell [grid x y]
  (-> grid (nth y) (nth x)))

(defn cell-empty? [cell]
  (and
   (not (:occupied cell))
   (not (:falling cell))))

(comment
  (nth [[0 1 2]] 0))

(defn move-piece-down [db]
  (let [grid (-> db :game-state :grid)
        all-cells (flatten grid)
        falling-piece (first (filter :falling all-cells))
        _ (println falling-piece)
        current-y (:y falling-piece)
        current-x (:x falling-piece)
        next-y (+ 1 current-y)
        can-move-down? (and
                        (> grid-height next-y)
                        (cell-empty? (get-cell grid current-x next-y)))]
    (if
      can-move-down?
      (let [updated-grid
            (update-in
             grid
             [current-y current-x]
             (fn [cell]
                (dissoc cell :falling)))
            updated-grid
            (update-in
             updated-grid
             [(+ 1 current-y) current-x]
             (fn [cell]
                (assoc cell :falling true)))
            updated-db
            (assoc-in db [:game-state :grid] updated-grid)]
        updated-db)

      (let [updated-grid
            (update-in
             grid
             [current-y current-x]
             (fn [cell]
               (-> cell
                   (dissoc :falling)
                   (assoc :occupied true))))
            updated-db
            (assoc-in db [:game-state :grid] updated-grid)]
        (println "can't move down")
        updated-db))))

(defn step-falling [db]
  (let [grid (-> db :game-state :grid)
        all-cells (flatten grid)
        falling-piece? (seq (filter :falling all-cells))]
    (println falling-piece?)
    (cond
      ;; TODO lose condition if a new piece can't be added
      falling-piece?
      (move-piece-down db)

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

(defn cell [{:keys [falling occupied]}]
 ^{:key (str (random-uuid))}
 [:div
  {:style
   {:max-width "30px"
    :max-height "30px"
    :width "30px"
    :height "30px"
    :background (cond falling "coral"
                      occupied "gray"
                      true "powderblue")
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

