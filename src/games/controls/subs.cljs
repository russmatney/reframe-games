(ns games.controls.subs
  (:require [re-frame.core :as rf]))

;; TODO clean up walking across these named game-dbs
(rf/reg-sub
  ::game-grid
  (fn [db [_ game-opts]]
    (-> db :controls-games (get (:name game-opts)) :game-grid)))
