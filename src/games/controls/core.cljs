(ns games.controls.core
  (:require
   [games.grid.core :as grid]
   [games.controls.db :as controls.db]))

;; TODO explore control 'profiles' - premade and byo
;; supporting that might mean editable controls for free

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; controls->re-pressed helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn controls->rp-event-keys
  "Converts the passed controls-db into a
  re-pressed `[[::event][kbd1][kbd2]]` list.
  "
  [controls]
  (into
    []
    (map
      (fn [[_ {:keys [event keys]}]]
        (into
          []
          (cons
            event
            (map (fn [k]
                   (when-not k
                     (print "Alert! Unsupported key passed for event " event))
                   [(controls.db/key-label->re-pressed-key k)])
                 keys))))
      controls)))

(defn controls->rp-all-keys
  "Converts the passed controls-db into a
  re-pressed `[[kbd1][kbd2]]` list.
  "
  [controls]
  (into []
        (mapcat
          (fn [[_ {:keys [keys]}]]
            (map controls.db/key-label->re-pressed-key keys))
          controls)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls Game Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def shapes
  "Shapes added to the controls game."
  [{:props {:move true}
    :cells [{:y -1} {:x -1} {:anchor true} {:x 1}]}])

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
  [grid]
  (let [ec->cell-fns (map shape->cells shapes)]
    (print ec->cell-fns)
    (reduce
      (fn [grid cell-fn]
        (grid/add-cells grid {:make-cells cell-fn}))
      grid
      ec->cell-fns)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Move Logic
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn can-player-move? [db] true)

(defn move-piece [db dir]
  db)

(defn rotate-piece [db] db)
