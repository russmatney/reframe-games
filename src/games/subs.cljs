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

(rf/reg-sub
 ::grid-for-display
 :<- [::tetris-db]
 (fn [{:keys [grid]}]
   (filter (fn [row] (<= 0 (-> row (first) :y))) grid)))
