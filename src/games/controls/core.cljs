(ns games.controls.core
  (:require
   [games.grid.core :as grid]))

;; TODO explore control 'profiles' - premade and byo
;; supporting that might mean editable controls for free

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls Game Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def shapes
  "Shapes added to the controls game."
  [
   ;; {:props {:moveable? true}
   ;;  :cells [{:y -1} {:x -1} {:anchor? true} {:x 1} {:y 1}]}
   {:props {:moveable? true}
    :cells [{:y -1} {:x -1} {:anchor? true}]}
   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Adding Pieces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn shape->cells [{:keys [cells props]}]
  (fn [ec] (map
             (fn [c]
               (as-> c cell
                 (merge props cell)
                 (grid/relative ec cell)))
             cells)))

(defn add-pieces
  [db]
  (let [ec->cell-fns (map shape->cells shapes)]
    (update db :game-grid
            (fn [g]
              (reduce
                (fn [grid cell-fn]
                  (grid/add-cells grid {:make-cells cell-fn}))
                g
                ec->cell-fns)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Move Logic
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn move-allowed?
  "True if user-interaction with the piece is allowed
  (i.e. the game is not paused or otherwise disabled.)"
  [_db]
  true)

(defn move-piece
  "Gathers `:moveable?` cells and moves them with `grid/move-cells`"
  [{:keys [game-grid game-opts] :as db} dir]
  (let [move-f         #(grid/move-cell-coords
                          % dir
                          (merge game-opts {:grid game-grid}))
        moveable-cells (grid/get-cells game-grid :moveable?)
        updated-grid   (grid/move-cells
                         game-grid
                         {:move-f    move-f
                          :cells     moveable-cells
                          :can-move? (fn [_] true)})
        db             (assoc db :game-grid updated-grid)]
    db))

(defn instant-down
  "Gathers `:moveable?` cells and moves them with `grid/instant-down`"
  [{:keys [game-grid] :as db}]
  (let [moveable-cells (grid/get-cells game-grid :moveable?)
        updated-grid   (grid/instant-down
                         game-grid
                         {:cells       moveable-cells
                          :keep-shape? true
                          :can-move?   ;; can the cell be merged into?
                          (fn [_] true)})
        db             (assoc db :game-grid updated-grid)]
    db))

(defn rotate-piece [{:keys [game-grid] :as db}]
  (let [cells       (grid/get-cells game-grid :props)
        anchor-cell (first (filter :anchor? cells))]
    (if-not anchor-cell
      ;; no anchor-cell, do nothing
      db
      (update db :game-grid
              (fn [grid]
                (grid/move-cells
                  grid
                  {:move-f    #(grid/calc-rotate-target anchor-cell %)
                   :can-move? (fn [_] true)
                   :cells     (remove :anchor? cells)}))))))
