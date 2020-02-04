(ns games.puyo.core
  (:require
   [games.puyo.db :as puyo.db]
   [games.grid.core :as grid]))

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
  "Returns true if any cell of the grid has a y < 0."
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
(defn rotate-piece
  [{:keys [game-grid] :as db}]
  (let [falling-cells (get-falling-cells db)
        anchor-cell (first (filter :anchor falling-cells))]

    (if-not anchor-cell
      ;; no anchor-cell, do nothing
      db
      (update db :game-grid
              (fn [grid]
                (grid/move-cells
                 grid
                 {:move-f #(calc-rotate-target anchor-cell %)
                  :fallback-moves
                  [{:additional-cells [anchor-cell]
                    :fallback-move-f (fn [c]
                                       (as-> c c
                                         (grid/move-cell-coords c :right)
                                         (calc-rotate-target
                                          (update anchor-cell :x inc) c)))}
                   {:additional-cells [anchor-cell]
                    :fallback-move-f (fn [c]
                                       (as-> c c
                                         (grid/move-cell-coords c :left)
                                         (calc-rotate-target
                                          (update anchor-cell :x dec) c)))}]
                  :can-move? #(cell-open? db %)
                  :cells (remove :anchor falling-cells)}))))))

(defn move-piece
  [{:keys [game-grid] :as db} direction]
  (let [falling-cells (get-falling-cells db)
        move-f #(grid/move-cell-coords % direction)

        updated-grid
        (grid/move-cells game-grid
            {:move-f move-f
             :can-move? #(cell-open? db %)
             :cells falling-cells})

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

;; TODO dry up
(defn clear-falling-cells [db]
  db)

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
                              {:entry-cell entry-cell
                               :update-cell #(assoc % :falling true)
                               :make-cells make-cells}))))))

(defn add-preview-piece [db shape-fn]
  db)

(defn groups-to-clear? [db]
  false)

(defn update-score [db]
  db)

(defn clear-groups [db]
  db)

(defn step [db]
  (cond
    ;; clear puyo groups, update db and return
    (groups-to-clear? db)
    (-> db
      (update-score)
      (clear-groups))

    ;; game is over, update db and return
    ;;(gameover? db) (assoc db :gameover? true)
    (gameover? db) puyo.db/initial-db

    ;; a piece is falling, move it down
    (any-falling? db)
    (move-piece db :down)

    ;; nothing is falling, add a new piece
    (not (any-falling? db))
    (add-new-piece db)

    ;; do nothing
    true db))

