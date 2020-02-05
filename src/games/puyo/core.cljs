(ns games.puyo.core
  (:require
   [games.puyo.db :as puyo.db]
   [games.grid.core :as grid]
   [clojure.walk :as walk]
   [clojure.set :as set]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game logic, predicates, helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cell-occupied? [{:keys [game-grid]} cell]
  (:occupied (grid/get-cell game-grid cell)))

(defn cell-falling? [{:keys [game-grid]} cell]
  (:falling (grid/get-cell game-grid cell)))

(defn cell-open?
  "Returns true if the indicated cell is within the grid's bounds AND not
  occupied."
  [{:keys [game-grid] :as db} cell]
  (and
    (grid/within-bounds? game-grid cell)
    (not (cell-occupied? db cell))))

(defn any-falling?
  "Returns true if there is a falling cell anywhere in the grid."
  [{:keys [game-grid]}]
  (seq (grid/get-cells game-grid :falling)))

(defn get-falling-cells
  "Returns all cells with a `:falling true` prop"
  [{:keys [game-grid]}]
  (grid/get-cells game-grid :falling))

(defn mark-cell-occupied
  "Marks the passed cell (x, y) as occupied, dissoc-ing the :falling key.
  Returns an updated db."
  [db cell]
  (update db :game-grid
          #(grid/update-cell % cell
                             (fn [c] (-> c
                                         (assoc :occupied true)
                                         (dissoc :falling))))))

(defn gameover?
  "Returns true if any cell of the grid has a y < 0.
  TODO fix this! only true if entry cell blocked"
  [{:keys [game-grid]}]
  (grid/any-cell? game-grid (fn [{:keys [y occupied]}]
                              (and occupied
                                   (< y 0)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Piece movement
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn rotate-diff
  "x1 = y0
  y1 = -x0"
  [{:keys [x y] :as cell}]
  {:x y
   :y (* -1 x)})

(defn calc-diff [anchor-cell cell]
  {:x (- (:x anchor-cell) (:x cell))
   :y (- (:y anchor-cell) (:y cell))})

(defn apply-diff [anchor-cell cell]
  {:x (+ (:x anchor-cell) (:x cell))
   :y (+ (:y anchor-cell) (:y cell))})

(defn calc-rotate-target [anchor-cell cell]
  (apply-diff anchor-cell (rotate-diff (calc-diff anchor-cell cell))))

;; TODO dry up?
;; TODO think about swapping colors vs 'rotating', especially in narrow
;; situations
(defn rotate-piece
  [{:keys [game-grid] :as db}]
  (let [falling-cells (get-falling-cells db)
        anchor-cell   (first (filter :anchor falling-cells))]

    (if-not anchor-cell
      ;; no anchor-cell, do nothing
      db
      (update db :game-grid
              (fn [grid]
                (grid/move-cells
                  grid
                  {:move-f    #(calc-rotate-target anchor-cell %)
                   :fallback-moves
                   [{:additional-cells [anchor-cell]
                     :fallback-move-f  (fn [c]
                                         (as-> c c
                                           (grid/move-cell-coords c :right)
                                           (calc-rotate-target
                                             (update anchor-cell :x inc) c)))}
                    {:additional-cells [anchor-cell]
                     :fallback-move-f  (fn [c]
                                         (as-> c c
                                           (grid/move-cell-coords c :left)
                                           (calc-rotate-target
                                             (update anchor-cell :x dec) c)))}]
                   :can-move? #(cell-open? db %)
                   :cells     (remove :anchor falling-cells)}))))))

(defn move-piece
  [{:keys [game-grid] :as db} direction]
  (let [falling-cells (get-falling-cells db)
        move-f        #(grid/move-cell-coords % direction)

        updated-grid
        (grid/move-cells game-grid
                         {:move-f    move-f
                          :can-move? #(cell-open? db %)
                          :cells     falling-cells})

        db (assoc db :game-grid updated-grid)

        any-blocked-cells?
        (and
          ;; down-only
          (= direction :down)
          ;; any falling cells?
          falling-cells
          ;; any falling cells that can't move down?
          ;; i.e. with occupied cells below them
          (seq (remove #(cell-open? db (move-f %)) falling-cells)))]

    ;; TODO do not allow movement after first break or after cells are removed
    ;; TODO 'falling' pieces above other falling pieces lock a step later...
    (if any-blocked-cells?
      ;; mark blocked cells :occupied, remove :falling
      ;; Effectively a 'piece-played' event
      (as-> db db
        (reduce (fn [d cell]
                  (if (not (cell-open? d (move-f cell)))
                    (mark-cell-occupied d cell)
                    d))
                db falling-cells))
      ;; this also indicates that the pieces has been played, so we increment
      ;; (update db :pieces-played inc)
      ;; remove the hold-lock to allow another hold to happen
      ;; (assoc db :hold-lock false))

      ;; otherwise just return the db
      db)))

(defn clear-falling-cells
  "Supports the 'hold/swap' mechanic."
  [db]
  (update db :game-grid #(grid/clear-cells % :falling)))

(defn next-bag
  ;; TODO balance colors reasonably
  [db]
  (repeat 5 puyo.db/entry-cell->puyo))

(defn add-new-piece
  "Adds a new cell to the grid.
  Does not care if there is room to add it!
  Depends on the `new-piece-coord`."
  [{:keys [entry-cell game-grid piece-queue min-queue-size] :as db}]
  (let [make-cells (first piece-queue)]
    (-> db
        (update :piece-queue
                (fn [q]
                  (let [q (drop 1 q)]
                    (if (< (count q) min-queue-size)
                      (concat q (next-bag db))
                      q))))

        (assoc :falling-shape-fn make-cells)

        (update :game-grid
                (fn [g]
                  (grid/add-cells g
                                  {:entry-cell  entry-cell
                                   :update-cell #(assoc % :falling true)
                                   :make-cells  make-cells}))))))

(defn add-preview-piece [db shape-fn]
  db)

(defn- adjacent?
  "True if the cells are neighboring cells.
  Determined by having the same x and y +/- 1, or same y and x +/- 1.
  "
  [c0 c1]
  (let [{x0 :x y0 :y} c0
        {x1 :x y1 :y} c1]
    (or (and (= x0 x1)
             (or
               (= y0 (+ y1 1))
               (= y0 (- y1 1))))
        (and (= y0 y1)
             (or
               (= x0 (+ x1 1))
               (= x0 (- x1 1)))))))

;; TODO move to grid api?
(defn- group-adjacent-cells
  "Takes a collection of cells, returns a list of cells grouped by adjacency.
  See `adjacent?`."
  [cells]
  ;; iterate over cells
  ;; first cell - create set, add all adjacent cells from group
  ;; next cell - if in first cell, add all adjacent to that group
  ;;    else, add all adjacent to new set
  ;; iterate
  (reduce
    (fn [groups cell]
      (let [in-a-set?      (seq (filter #(contains? % cell) groups))
            adjacent-cells (set (filter #(adjacent? % cell) cells))]
        (if in-a-set?
          ;; in a set, so walk and update group in-place
          ;; probably a better way to do this kind of in-place update
          (walk/walk
            (fn [group]
              (if (contains? group cell)
                (set/union group adjacent-cells)
                group))
            identity
            groups)
          ;; otherwise, add a new set to groups
          (conj groups (conj adjacent-cells cell)))))
    []
    cells))

(defn groups-to-clear
  "Returns true if there are any groups of 4 or more adjacent same-color cells."
  [{:keys [game-grid group-size] :as db}]
  (let [puyos        (grid/get-cells game-grid :occupied)
        color-groups (vals (group-by :color puyos))
        groups       (mapcat group-adjacent-cells color-groups)]
    (filter (fn [group] (<= group-size (count group))) groups)))

(defn update-score [db]
  db)

(defn clear-groups
  "Clears groups that are have reached the group-size."
  [db groups]
  (update db :game-grid
          (fn [grid]
            (grid/update-cells
              grid
              (fn [cell]
                (seq (filter #(contains? % cell) groups)))
              #(dissoc % :occupied :color :anchor)))))

(defn deeper-y?
  [{y0 :y} {y1 :y}]
  (> y0 y1))

;; TODO grid helper?
(defn ->deepest-by-x
  [cells]
  (let [cols            (vals (group-by :x cells))
        deepest-per-col (map #(first (sort deeper-y? %))
                             cols)]
    (->>
      deepest-per-col
      (group-by :x)
      (map (fn [[k v]] [k (first v)]))
      (into {}))))

(defn update-fallers
  "Updates cells that have had a cell below removed."
  [db groups]
  (let [deepest-by-x (->deepest-by-x (reduce set/union groups))
        should-update?
        (fn [{:keys [color x y]}]
          (let [deep-y (:y (get deepest-by-x x))]
            (and
              color
              (not (nil? deep-y))
              (< y deep-y))))]
    (update db :game-grid
            (fn [grid]
              (grid/update-cells
                grid
                should-update?
                #(-> %
                     (dissoc :occupied)
                     (assoc :falling true)))))))

(defn step
  [db]
  (let [groups (groups-to-clear db)]
    (cond
      ;; game is over, update db and return
      ;;(gameover? db) (assoc db :gameover? true)
      (gameover? db) puyo.db/initial-db

      ;; a piece is falling, move it down
      (any-falling? db)
      (move-piece db :down)

      ;; clear puyo groups, update db and return
      (seq groups)
      (-> db
          (update-score)
          (clear-groups groups)
          (update-fallers groups))

      ;; nothing is falling, add a new piece
      (not (any-falling? db))
      (add-new-piece db)

      ;; do nothing
      true db)))

