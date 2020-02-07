(ns games.puyo.subs
  (:require
   [re-frame.core :as rf]
   [games.puyo.db :as puyo.db]
   [games.grid.core :as grid]
   [games.puyo.core :as puyo]))


(rf/reg-sub
  ::puyo-db
  (fn [db]
    (::puyo.db/db db)))

(rf/reg-sub
  ::current-view
  :<- [::puyo-db]
  (fn [db]
    (or (:current-view db) :game)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Grids
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
  ::game-grid
  :<- [::puyo-db]
  (fn [{:keys [game-grid]}]
    (grid/positive-rows (:grid game-grid))))

(rf/reg-sub
  ::preview-grids
  :<- [::puyo-db]
  (fn [db]
    (map :grid (:preview-grids db))))

(rf/reg-sub
  ::held-grid
  :<- [::puyo-db]
  (fn [{:keys [held-grid]}]
    (:grid held-grid)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Logic
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
  ::paused?
  :<- [::puyo-db]
  :paused?)

(rf/reg-sub
  ::gameover?
  :<- [::puyo-db]
  :gameover?)

(rf/reg-sub
  ::any-held?
  :<- [::puyo-db]
  (fn [{:keys [held-shape-fn]}]
    held-shape-fn))

(rf/reg-sub
  ::score
  :<- [::puyo-db]
  (fn [db]
    (:score db)))

(rf/reg-sub
  ::time
  :<- [::puyo-db]
  (fn [db]
    (:time db)))

(rf/reg-sub
  ::level
  :<- [::puyo-db]
  (fn [db]
    (:level db)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
  ::controls
  :<- [::puyo-db]
  (fn [db]
    (-> db :controls)))

(rf/reg-sub
  ::keys-for
  :<- [::controls]
  (fn [controls [_ keys-for]]
    (-> controls keys-for :keys)))
