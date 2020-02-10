(ns games.controls.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  ::controls-db
  (fn [db _]
    (:controls-db db)))

(rf/reg-sub
  ::grid
  :<- [::controls-db]
  (fn [db _]
    (:game-grid db)))
