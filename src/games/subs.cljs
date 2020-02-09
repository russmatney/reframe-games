(ns games.subs
  (:require
   [re-frame.core :as rf]))


(rf/reg-sub
  ::current-view
  (fn [db _]
    (:current-view db)))

(rf/reg-sub
  ::controls
  (fn [db _]
    (:controls db)))
