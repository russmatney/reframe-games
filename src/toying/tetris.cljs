(ns toying.tetris
  (:require
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [toying.events :as evts]
   [clojure.set :as set]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game constants

(def grid-phantom-rows 4)
(def grid-height 4)
(def grid-width 7)

(def new-piece-coord {:x 3 :y 0})
(def single-cell-shape [new-piece-coord])
(def three-cell-shape [{:x 1 :y 0}
                       {:x 2 :y 0 :anchor true}
                       {:x 3 :y 0}])
(def two-cell-shape [{:x 3 :y -1 :anchor true}
                     {:x 3 :y 0}])

(def allowed-shapes
    [two-cell-shape])
    ;; [single-cell-shape
    ;;  two-cell-shape
    ;;  three-cell-shape])


(defn reset-cell-labels [grid]
  (vec
   (map-indexed
    (fn [y row]
     (vec
      (map-indexed
       (fn [x cell]
        (assoc cell :y (- y grid-phantom-rows) :x x))
       row)))
    grid)))

(defn build-row []
  (vec (take grid-width (repeat {}))))

(defn build-grid []
  (reset-cell-labels
    (take (+ grid-height grid-phantom-rows) (repeat (build-row)))))

(def initial-game-state
  {:grid (build-grid)
   :phase :falling})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Updating cells

(defn update-cell
  "Applies the passed function to the cell at the specified coords."
  ([db cell f]
   (update-cell db (:x cell) (:y cell) f))
  ([db x y f]
   (let [grid (-> db :game-state :grid)
         updated (update-in grid [(+ grid-phantom-rows y) x] f)]
      (assoc-in db [:game-state :grid] updated))))

(defn mark-cell-occupied
  "Marks the passed cell (x, y) as occupied, dissoc-ing the :falling key.
  Returns an updated db."
  ([db {:keys [x y]}]
   (update-cell db x y
               #(-> %
                  (assoc :occupied true)
                  (dissoc :falling)))))

(defn mark-cell-falling
  "Marks the passed cell (x, y) as falling.
  Returns an updated db."
  ([db {:keys [x y]}]
   (update-cell db x y #(assoc % :falling true))))

(defn unmark-cell-falling
  "Removes :falling key from the passed cell (x, y).
  Returns an updated db."
  ([db {:keys [x y]}]
   (unmark-cell-falling db x y))
  ([db x y]
   (update-cell db x y #(dissoc % :falling))))

(defn get-cell
  ([grid {:keys [x y]}]
   (get-cell grid x y))
  ([grid x y]
   (-> grid (nth (+ y grid-phantom-rows)) (nth x))))

(defn copy-to-cell
  "Copies props from passed `cell` to `target`."
  ([db cell target]
   (let [grid (-> db :game-state :grid)
         props (as-> cell c
                   (get-cell grid c)
                   (dissoc c :x :y :falling))]
     (update-cell db target #(merge props %)))))

(defn clear-cell-props
  "Removes non-coordinate flags from cells."
  ([db cell]
   (update-cell db cell #(dissoc % :falling :anchor))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Predicates and their helpers

(defn cell-occupied? [cell]
   (:occupied cell))

(defn can-move?
  "Returns true if the indicated cell is not occupied or beyond the edge of the
  grid.

  Supports a cell and direction, or a coordinate

  "
  ([grid cell]
   (can-move? grid cell nil))
  ([grid {:keys [x y]} direction]
   (let [next-x (+ (case direction :right 1 :left -1 0) x)
         next-y (+ (case direction :down 1 0) y)]
     (and
      (> grid-height next-y)
      (> grid-width next-x)
      (>= next-x 0)
      (not (cell-occupied? (get-cell grid next-x next-y)))))))

(defn can-add-new? [grid]
  (let [{:keys [y x]} new-piece-coord
        entry-cell (get-cell grid x y)]
    (or
     (not (cell-occupied? entry-cell))
     (can-move? grid entry-cell :down))))

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

(defn any-falling?
  "Returns true if there is a falling cell anywhere in the grid."
  [db]
  (let [grid (-> db :game-state :grid)
        all-cells (flatten grid)]
   (seq (filter :falling all-cells))))

(defn gameover?
  "Returns true if no new pieces can be added.
  Needs to know what the next piece is, doesn't it?
  TODO generate list of pieces to be added, pull from the db here
  "
  [db]
  (let [grid (-> db :game-state :grid)]
   (not (can-add-new? grid))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Updating the board

(defn get-falling-cells [db]
  (let [grid (-> db :game-state :grid)
        all-cells (flatten grid)]
    (seq (filter :falling all-cells))))

;; TODO consider move-cell and cell/grid apis
;; could simplify all this
(defn move-piece
  "Moves a floating cell in the direction passed.
  If pieces try to move down but are blocked, they are locked in place (with an
  :occupied flag).

  TODO use copy-to-cell to copy cell props from a to b, and clear-cell on b
  "
  [db direction]
  (let [grid (-> db :game-state :grid)
        falling-cells (get-falling-cells db)
        current-coords (set (map (fn [{:keys [x y]}]
                                   {:x x :y y})
                                 falling-cells))
        x-diff (case direction :left -1 :right 1 0)
        y-diff (case direction :down 1 0)
        next-coords (set
                     (map (fn [{:keys [x y]}]
                           {:x (+ x x-diff)
                            :y (+ y y-diff)})
                          current-coords))
        coords-to-unmark (set/difference current-coords next-coords)]
    (cond
      ;; all falling pieces can move
      (empty? (seq (remove #(can-move? grid % direction) falling-cells)))
      ;; move all falling pieces in direction
      (as-> db db
        (reduce unmark-cell-falling db coords-to-unmark)
        (reduce mark-cell-falling db next-coords))

      ;; if we try to move down but we can't, lock all falling pieces in place
      (and
       ;; down-only
       (= direction :down)
       ;; any falling cells?
       falling-cells
       ;; any falling cells that can't move down?
       ;; i.e. with occupied cells below them
       (seq (remove #(can-move? grid % :down) falling-cells)))
      ;; mark all cells
      (reduce (fn [db cell] (mark-cell-occupied db cell))
              db falling-cells)

      ;; otherwise just return the db
      true db)))

(defn select-new-piece []
  (rand-nth allowed-shapes))

(defn add-new-piece
  "Adds a new cell to the grid.
  Does not care if there is room to add it!
  Depends on the `new-piece-coord`.
  "
  [db]
  (let [new-cells (select-new-piece)]
     (reduce
        (fn [db {:keys [x y] :as cell}]
          (-> db
           (mark-cell-falling cell)
           (copy-to-cell cell {:x x :y y})))
        db
        new-cells)))

(defn rotate-coord [[x y]]
  [y (* -1 x)])

(defn calc-diff [anchor-cell cell]
  {:x (- (:x anchor-cell) (:x cell))
   :y (- (:y anchor-cell) (:y cell))})

(defn apply-diff [anchor-cell cell]
  {:x (+ (:x anchor-cell) (:x cell))
   :y (+ (:y anchor-cell) (:y cell))})

(defn rotate-piece
  "Rotates a falling piece in-place.
  This requires one falling cell to be an 'anchor'.
  The anchor stays in place - the other cells calc an x/y delta between
  themselves and the anchor.

  Maybe this rule works:
  x1 = y0
  y1 = -x0
  "
  [db]
  (let [grid (-> db :game-state :grid)
        falling-cells (get-falling-cells db)
        anchor-cell (seq (filter :anchor falling-cells))]

    (cond
      ;; no anchor-cell, do nothing
      (not anchor-cell)
      db

     true
     (let [to-rotate (seq (remove :anchor falling-cells))
           diffs (map #(calc-diff anchor-cell %) to-rotate)
           diffs-to-rotate (set (map (fn [c] [(:x c) (:y c)]) diffs))
           rotated-diffs (set (map rotate-coord diffs-to-rotate))
           diffs-to-unmark
           (set/difference diffs-to-rotate rotated-diffs)
           rotated-coords (map #(apply-diff anchor-cell %) rotated-diffs)
           coords-to-unmark (map #(apply-diff diffs-to-unmark %) diffs-to-unmark)]
       (print "rotating!")
       (cond
         (seq (remove #(can-move? grid %) rotated-coords))
         db

         true
         (as-> db db
           ;; TODO do a proper copy cell here
           (reduce unmark-cell-falling db coords-to-unmark)
           (reduce mark-cell-falling db rotated-coords)))))))


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
    (any-falling? db)
    (move-piece db :down)

    ;; nothing is falling, add a new piece
    (not (any-falling? db))
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
                 [{:keyCode 74}]] ;; j key
                [[:space-pressed]
                 [{:keyCode 32}]]]}]) ;; space bar

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

(rf/reg-event-fx
 :space-pressed
 (fn [{:keys [db]} _ _]
   (let [tetris-db (::tetris db)
         updated-tetris-db (rotate-piece tetris-db)]
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
 ::grid-for-display
 :<- [::game-state]
 (fn [game-state]
   (filter (fn [row] (<= 0 (:y (first row)))) (:grid game-state))))

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
  (let [grid @(rf/subscribe [::grid-for-display])
        gameover? @(rf/subscribe [::gameover?])]
   [:div
    (if gameover? "GAMEOVER"
     (for [row grid]
       ^{:key (str (random-uuid))}
       [:div
        {:style {:display "flex"}}
        (for [cell-state row]
         (cell cell-state))]))]))

