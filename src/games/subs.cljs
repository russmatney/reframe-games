(ns games.subs
  (:require
   [re-frame.core :as rf]))


(rf/reg-sub
  ::current-page
  (fn [db _]
    (:current-page db)))

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
