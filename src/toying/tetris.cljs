(ns toying.tetris
 (:require
  [re-frame.core :as rf]
  [toying.events :as evts]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game constants

(def grid-height 5)
(def grid-width 3)

(def new-piece-coord [0 1])

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
        (update-in grid new-piece-coord
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

(defn can-move-down? [grid cell]
  (let [cell-y (:y cell)
        cell-x (:x cell)
        next-y (+ 1 cell-y)]
     (and
       (> grid-height next-y)
       (cell-empty? (get-cell grid cell-x next-y)))))

(defn can-add-new? [grid]
  (let [[y x] new-piece-coord
        cell (get-cell grid x y)]
    (or
     (cell-empty? cell)
     (can-move-down? grid cell))))

(defn move-piece-down [db]
  (let [grid (-> db :game-state :grid)
        all-cells (flatten grid)
        falling-piece (first (filter :falling all-cells))
        current-y (:y falling-piece)
        current-x (:x falling-piece)
        next-y (+ 1 current-y)]
    (if
      (can-move-down? grid falling-piece)
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
        updated-db))))

(defn step-falling [db]
  (let [grid (-> db :game-state :grid)
        all-cells (flatten grid)
        falling-piece? (seq (filter :falling all-cells))
        gameover? (not (can-add-new? grid))]
    (cond
      ;; gameover? (assoc db :gameover true)
      gameover? (assoc db :game-state initial-game-state)

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

(rf/reg-sub
 ::gameover?
 :<- [::tetris-db]
 (fn [db]
   (:gameover db)))

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
  (let [game-state @(rf/subscribe [::game-state])
        gameover? @(rf/subscribe [::gameover?])]
   [:div
    (if gameover? "GAMEOVER"
     (for [row (:grid game-state)]
       ^{:key (str (random-uuid))}
       [:div
        {:style {:display "flex"}}
        (for [cell-state row]
         (cell cell-state))]))]))

