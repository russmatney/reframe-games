(ns toying.subs
  (:require
   [re-frame.core :as rf]
   [toying.tetris :as tetris]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Subs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
 ::tetris-db
 (fn [db]
   (::tetris/db db)))

(rf/reg-sub
 ::game-state
 :<- [::tetris-db]
 (fn [db]
   (:game-state db)))

(rf/reg-sub
 ::grid-for-display
 :<- [::game-state]
 (fn [game-state]
   (filter (fn [row] (<= 0 (:y (first row)))) (:grid game-state))))
