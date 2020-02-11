(ns games.controls.subs
  (:require [re-frame.core :as rf]))

(defn ->db
  ([db game-opts]
   (-> db :controls-games (get (:name game-opts))))
  ([db game-opts k]
   (-> db :controls-games (get (:name game-opts)) k)))

(rf/reg-sub
  ::game-grid
  (fn [db [_ game-opts]]
    (->db db game-opts :game-grid)))

(rf/reg-sub
  ::game-opts
  (fn [db [_ game-opts]]
    (->db db game-opts :game-opts)))

(rf/reg-sub
  ::debug?
  (fn [db [_ game-opts]]
    (:debug? (->db db game-opts :game-opts))))

(rf/reg-sub
  ::debug-game-opts
  (fn [db]
    (let [games     (vals (:controls-games db))
          game-opts (map :game-opts games)]
      (filter :debug-game? game-opts))))
