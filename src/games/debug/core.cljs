(ns games.debug.core
  (:require [games.grid.core :as grid]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Shapes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def shapes
  "Shapes added to the debug game."
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

(defn is-space? [cell]
  (not (:moveable? cell)))

(defn instant-fall
  "Gathers `:moveable?` cells and moves them with `grid/instant-fall`"
  [{:keys [game-grid game-opts] :as db} direction]
  (update
    db :game-grid
    (fn [g]
      (grid/instant-fall
        g
        {:direction   direction
         :cells       (grid/get-cells game-grid :moveable?)
         :keep-shape? (or false (:keep-shape? game-opts))
         :can-move?   is-space?}))))

(defn rotate-piece [{:keys [game-grid] :as db}]
  (let [cells       (grid/get-cells game-grid :props)
        anchor-cell (first (filter :anchor? cells))]
    (if-not anchor-cell
      ;; no anchor-cell, do nothing
      db
      (update
        db :game-grid
        (fn [grid]
          (grid/move-cells
            grid
            {:move-f    #(grid/calc-rotate-target anchor-cell %)
             :can-move? (fn [_] true)
             :cells     (remove :anchor? cells)}))))))
