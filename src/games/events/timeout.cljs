(ns games.events.timeout
 (:require
  [reagent.core :as reagent]
  [re-frame.core :as rf]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Timeout logic and impl ripped from:
;; https://purelyfunctional.tv/guide/timeout-effect-in-re-frame/
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce timeouts (reagent/atom {}))

(rf/reg-fx
  :timeout
  (fn [{:keys [id event time]}]
    (when-some [existing (get @timeouts id)]
      (js/clearTimeout existing)
      (swap! timeouts dissoc id))
    (when (some? event)
      (swap! timeouts assoc id
        (js/setTimeout
          (fn []
            (rf/dispatch event))
          time)))))

(rf/reg-fx
  :clear-timeout
  (fn [{:keys [id]}]
    (when-some [existing (get @timeouts id)]
      (js/clearTimeout existing)
      (swap! timeouts dissoc id))))
