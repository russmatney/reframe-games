(ns games.subs
  (:require
   [re-frame.core :as rf]
   [games.grid.core :as grid]))


(rf/reg-sub
  ::current-page
  (fn [db _]
    (:current-page db)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Game shared subs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
  ::game-db
  (fn [db evt]
    (case (count evt)
      2 (let [[_e game-opts] evt]
          (-> db :games (get (:name game-opts))))
      3 (let [[_e game-opts k] evt]
          (-> db :games (get (:name game-opts)) (get k))))))

(rf/reg-sub
  ::game-opts
  (fn [db [_ game-opts]]
    (-> db :games (get (:name game-opts)) :game-opts)))

(rf/reg-sub
  ::game-grid
  (fn [db [_ game-opts]]
    (-> db :games (get (:name game-opts)) :game-grid
        (grid/only-positive-rows))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
  ::controls
  (fn [db]
    (-> db :controls)))

(rf/reg-sub
  ::controls-for
  :<- [::controls]
  (fn [controls [_ for]]
    (let [controls-by-id (group-by :id controls)]
      (-> controls-by-id for first))))

(rf/reg-sub
  ::keys-for
  :<- [::controls]
  (fn [controls [_ for]]
    (let [controls-by-id (group-by :id controls)]
      (-> controls-by-id for first :keys))))

;; TODO fix these `first` uses
;; probably a controls revamp
(rf/reg-sub
  ::event-for
  :<- [::controls]
  (fn [controls [_ for]]
    (let [controls-by-id (group-by :id controls)]
      (-> controls-by-id for first :event))))
