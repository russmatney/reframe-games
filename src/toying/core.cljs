(ns toying.core
  (:require
   [reagent.core :as reagent]
   [toying.views :as views]))

(defn dev-setup []
  (enable-console-print!)
  (println "dev mode"))

(defn mount-root []
  (reagent/render [views/root]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (dev-setup)
  (mount-root))
