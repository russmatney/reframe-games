(ns games.tetris.subs
  (:require
   [re-frame.core :as rf]
   [games.tetris.db :as tetris.db]
   [games.tetris.core :as tetris]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Subs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
 ::tetris-db
 (fn [db]
   (::tetris.db/db db)))

(defn positive-rows
  [grid]
  (filter (fn [row] (<= 0 (-> row (first) :y))) grid))

(rf/reg-sub
 ::game-grid
 :<- [::tetris-db]
 (fn [{:keys [game-grid]}]
   (positive-rows (:grid game-grid))))

(rf/reg-sub
 ::preview-grids
 :<- [::tetris-db]
 (fn [db]
   (map :grid (:preview-grids db))))

(rf/reg-sub
 ::held-grid
 :<- [::tetris-db]
 (fn [{:keys [held-grid]}]
   (:grid held-grid)))

(rf/reg-sub
 ::score
 :<- [::tetris-db]
 (fn [db]
   (:score db)))

(rf/reg-sub
 ::time
 :<- [::tetris-db]
 (fn [db]
   (:time db)))

(rf/reg-sub
 ::level
 :<- [::tetris-db]
 (fn [db]
   (:level db)))

;; a hacky here, shouldn't be creating things on the fly in subs...
(rf/reg-sub
 ::allowed-piece-grids
 :<- [::tetris-db]
 (fn [db]
   (map (fn [shape-fn]
          (:grid (tetris/add-preview-piece {:height 5 :width 5} shape-fn)))
        (:allowed-shape-fns db))))
