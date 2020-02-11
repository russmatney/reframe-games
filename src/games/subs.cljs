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
    (-> controls for)))

(rf/reg-sub
  ::keys-for
  :<- [::controls]
  (fn [controls [_ keys-for]]
    (-> controls keys-for :keys)))

(rf/reg-sub
  ::event-for
  :<- [::controls]
  (fn [controls [_ event-for]]
    (-> controls event-for :event)))
