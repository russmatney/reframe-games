(ns games.subs
  (:require
   [re-frame.core :as rf]))


(rf/reg-sub
  ::selected-game
  (fn [db _]
    (:selected-game db)))


