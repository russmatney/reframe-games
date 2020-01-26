(ns games.events.timeout
 (:require
  [reagent.core :as reagent]
  [re-frame.core :as rf]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Timeout logic and impl ripped from:
;; https://purelyfunctional.tv/guide/timeout-effect-in-re-frame/
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce timeouts (reagent/atom {}))

(defn handle-timeout
  [{:keys [id event time]}]
  (when-some [existing (get @timeouts id)]
    (js/clearTimeout existing)
    (swap! timeouts dissoc id))
  (when (some? event)
    (swap! timeouts assoc id
      (js/setTimeout
        (fn []
          (rf/dispatch event))
        time))))

(defn clear-timeout
  [{:keys [id]}]
  (when-some [existing (get @timeouts id)]
    (js/clearTimeout existing)
    (swap! timeouts dissoc id)))

(rf/reg-fx
  :timeout
  handle-timeout)

(rf/reg-fx
  :timeouts
  (fn [ts] (doall (map handle-timeout ts))))

(rf/reg-fx
  :clear-timeout
  clear-timeout)

(rf/reg-fx
  :clear-timeouts
  (fn [xs]
    (doall (map clear-timeout xs))))
