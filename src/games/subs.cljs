(ns games.subs
  (:require
   [re-frame.core :as rf]
   [games.tetris.db :as tetris.db]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Subs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; TODO move these to games.tetris.subs?

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
 ::preview-grid
 :<- [::tetris-db]
 (fn [db]
   (positive-rows (:grid (:preview-grid db)))))
