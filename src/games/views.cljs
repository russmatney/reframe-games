(ns games.views
  (:require
   [reagent.core :as reagent]
   [games.tetris.views :as tetris.views]))

(defn root []
  [:div#root
   [tetris.views/page]])


