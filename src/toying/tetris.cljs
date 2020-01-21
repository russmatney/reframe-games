(ns toying.tetris
 (:require
  [re-frame.core :as rf]
  [re-pressed.core :as rp]
  [toying.events :as evts]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game constants

(def grid-height 4)
(def grid-width 7)

(def new-piece-coord [0 3])


(defn reset-cell-labels [grid]
  (vec
   (map-indexed
    (fn [y row]
     (vec
      (map-indexed
       (fn [x cell]
        (assoc cell :y y :x x))
       row)))
    grid)))

(defn build-row []
  (vec (take grid-width (repeat {}))))

(defn build-grid []
  (reset-cell-labels
    (take grid-height (repeat (build-row)))))

(def initial-game-state
  {:grid (build-grid)
   :phase :falling})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game functions

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell predicates and helpers

(defn get-cell [grid x y]
  (-> grid (nth y) (nth x)))

(defn cell-empty? [cell]
  (and
   (not (:occupied cell))
   (not (:falling cell))))

(defn can-move? [direction grid cell]
  (let [cell-y (:y cell)
        cell-x (:x cell)
        next-x (+ (case direction :right 1 :left -1 0) cell-x)
        next-y (+ (case direction :down 1 0) cell-y)]
     (and
      (or (and
           (= direction :down)
           (> grid-height next-y))
          (and
           (or (= direction :right) (= direction :left))
           (> grid-width next-x)
           (>= next-x 0)))
      (cell-empty? (get-cell grid next-x next-y)))))

(defn can-add-new? [grid]
  (let [[y x] new-piece-coord
        cell (get-cell grid x y)]
    (or
     (cell-empty? cell)
     (can-move? :down grid cell))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Move piece

(defn move-piece [tetris-db direction]
  (let [grid (-> tetris-db :game-state :grid)
        all-cells (flatten grid)
        falling-piece (first (filter :falling all-cells))
        current-y (:y falling-piece)
        current-x (:x falling-piece)
        next-x (+ (case direction :left -1 :right 1 0) current-x)
        next-y (if (= :down direction) (+ 1 current-y) current-y)]
    (cond
      (can-move? direction grid falling-piece)
      (let [updated-grid
            (update-in
             grid
             [current-y current-x]
             (fn [cell]
                (dissoc cell :falling)))
            updated-grid
            (update-in
             updated-grid
             [next-y next-x]
             (fn [cell]
                (assoc cell :falling true)))
            updated-db
            (assoc-in tetris-db [:game-state :grid] updated-grid)]
        updated-db)

      ;; if we try to move down but we can't, lock the piece in place
      (= direction :down)
      (let [updated-grid
            (update-in
             grid
             [current-y current-x]
             (fn [cell]
               (-> cell
                   (dissoc :falling)
                   (assoc :occupied true))))
            updated-db
            (assoc-in tetris-db [:game-state :grid] updated-grid)]
        updated-db)

      ;; otherwise just return the db
      true tetris-db)))


(defn add-new-piece [db]
  (let [grid (-> db :game-state :grid)
        updated-grid
        (update-in grid new-piece-coord
          (fn [cell]
            (assoc cell :falling true)))
        updated-db
        (assoc-in db [:game-state :grid] updated-grid)]
    updated-db))

(defn row-fully-occupied? [row]
   (= (count row)
      (count (seq (filter :occupied row)))))

(defn rows-to-clear?
  "Returns true if there are rows to be removed from the board."
  [db]
  (let [grid (-> db :game-state :grid)]
    (seq (filter row-fully-occupied? grid))))

(defn clear-full-rows [db]
  (let [grid (-> db :game-state :grid)
        cleared-grid (seq (remove row-fully-occupied? grid))
        rows-to-add (- grid-height (count cleared-grid))
        new-rows (take rows-to-add (repeat (build-row)))
        grid-with-new-rows (concat new-rows cleared-grid)
        updated-grid (reset-cell-labels grid-with-new-rows)]
    (assoc-in db [:game-state :grid] updated-grid)))

(defn falling-piece?
  "Returns true if there is a falling piece anywhere in the grid."
  [db]
  (let [grid (-> db :game-state :grid)
        all-cells (flatten grid)
        falling-piece? (seq (filter :falling all-cells))]
   (seq (filter :falling all-cells))))

(defn gameover?
  "Returns true if no new pieces can be added."
  [db]
  (let [grid (-> db :game-state :grid)]
   (not (can-add-new? grid))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game tick/steps functions

(defn step-falling [db]
  (cond
    ;; game is over, update db and return
    ;;(gameover? db) (assoc db :gameover true) ;; or :phase :gameover?
    (gameover? db) (assoc db :game-state initial-game-state)

    ;; clear pieces, update db and return
    (rows-to-clear? db) ;; animation?
    (clear-full-rows db)

    ;; a piece is falling, move it down
    (falling-piece? db)
    (move-piece db :down)

    ;; nothing is falling, add a new piece
    (not (falling-piece? db))
    (add-new-piece db)

    ;; do nothing
    true db))

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

;; init
(rf/reg-event-db
 ::init-db
 (fn [db]
   (assoc db ::tetris initial-db)))

;; this is not quite right, but meh for now
(rf/dispatch-sync [::init-db])


;; game-tick
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


;; keypresses
(rf/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])

(rf/dispatch
 [::rp/set-keydown-rules
  {;; takes a collection of events followed by key combos that can trigger the event
   :event-keys [[[:enter-pressed]
                 [{:keyCode 13}]] ;; enter key
                [[:left-pressed]
                 [{:keyCode 37}] ;; left arrow
                 [{:keyCode 72}]] ;; h key
                [[:right-pressed]
                 [{:keyCode 39}] ;;right arrow
                 [{:keyCode 76}]] ;; l key
                [[:down-pressed]
                 [{:keyCode 38}] ;; down arrow
                 [{:keyCode 74}]]]}]) ;; j key

(rf/reg-event-fx
 :left-pressed
 (fn [{:keys [db]} _ _]
   (let [tetris-db (::tetris db)
         updated-tetris-db (move-piece tetris-db :left)]
    {:db (assoc db ::tetris updated-tetris-db)})))

(rf/reg-event-fx
 :right-pressed
 (fn [{:keys [db]} _ _]
   (let [tetris-db (::tetris db)
         updated-tetris-db (move-piece tetris-db :right)]
    {:db (assoc db ::tetris updated-tetris-db)})))

(rf/reg-event-fx
 :down-pressed
 (fn [{:keys [db]} _ _]
   (let [tetris-db (::tetris db)
         updated-tetris-db (move-piece tetris-db :down)]
    {:db (assoc db ::tetris updated-tetris-db)})))

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

