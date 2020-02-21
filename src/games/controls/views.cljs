(ns games.controls.views
  (:require
   [re-frame.core :as rf]
   [games.subs :as subs]
   [games.views.components :as components]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls-mini
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mini-text
  "Lists passed controls in text.
  The expected controls passed are the control's `:id`"
  [{:keys [controls]}]
  [components/widget
   {:style
    {:padding "0.9rem"
     :flex    "1"}}
   (doall
     (for [ctr controls]
       (let [{:keys [label event keys]}
             @(rf/subscribe [::subs/controls-for ctr])]
         (when (and keys event)
           ^{:key label}
           [:p
            {:style    {:margin-bottom "0.3rem"}
             :on-click #(rf/dispatch event)}
            (str label " (" (first keys) ")")]))))])

