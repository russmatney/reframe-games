(ns toying.subs
  (:require
   [re-frame.core :as rf]
   [toying.tetris.db :as tetris.db]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Subs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; TODO move these to toying.tetris.subs?

(rf/reg-sub
 ::tetris-db
 (fn [db]
   (::tetris.db/db db)))

(rf/reg-sub
 ::grid-for-display
 :<- [::tetris-db]
 (fn [{:keys [grid]}]
   (filter #(<= 0 (-> % (first) :y))) grid))
