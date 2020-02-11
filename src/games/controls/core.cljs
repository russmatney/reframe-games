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
  [{:props {:moveable? true}
    :cells [{:y -1} {:x -1} {:anchor? true} {:x 1} {:y 1}]}])

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

(defn add-piece
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

(defn can-player-move?
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

(defn rotate-piece [db] db)
