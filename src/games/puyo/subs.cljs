(ns games.puyo.subs
  (:require
   [re-frame.core :as rf]
   [games.puyo.db :as puyo.db]
   [games.puyo.core :as puyo]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Subs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
 ::puyo-db
 (fn [db]
   (::puyo.db/db db)))

;; TODO Move to grid namespace, dedupe
(defn positive-rows
  [grid]
  (filter (fn [row] (<= 0 (-> row (first) :y))) grid))

(rf/reg-sub
 ::game-grid
 :<- [::puyo-db]
 (fn [{:keys [game-grid]}]
   (positive-rows (:grid game-grid))))
