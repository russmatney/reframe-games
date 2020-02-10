(ns games.controls.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  ::grid
  (fn [db _]
    (:grid db)))
