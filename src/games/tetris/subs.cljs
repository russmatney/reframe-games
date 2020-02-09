(ns games.tetris.subs
  (:require
   [re-frame.core :as rf]
   [games.tetris.db :as tetris.db]
   [games.grid.core :as grid]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Subs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
  ::tetris-db
  (fn [db evt]
    (case (count evt)
      1 (-> db ::tetris.db/db :default)
      2 (let [[_e n] evt] (-> db ::tetris.db/db (get n))))))

(rf/reg-sub
  ::paused?
  :<- [::tetris-db]
  :paused?)

(rf/reg-sub
  ::gameover?
  :<- [::tetris-db]
  :gameover?)

(rf/reg-sub
  ::game-grid
  :<- [::tetris-db]
  (fn [{:keys [game-grid]}]
    (grid/only-positive-rows game-grid)))

(rf/reg-sub
  ::preview-grids
  :<- [::tetris-db]
  (fn [db]
    (:preview-grids db)))

(rf/reg-sub
  ::held-grid
  :<- [::tetris-db]
  (fn [{:keys [held-grid]}]
    held-grid))

(rf/reg-sub
  ::any-held?
  :<- [::tetris-db]
  (fn [{:keys [held-shape-fn]}]
    held-shape-fn))

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
