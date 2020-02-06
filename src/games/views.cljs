(ns games.views
  (:require
   [games.tetris.views :as tetris.views]
   [games.puyo.views :as puyo.views]))

(defn root []
  [:div#root
   ;; [tetris.views/page]
   [puyo.views/page]
   ])


