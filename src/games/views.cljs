(ns games.views
  (:require
   [games.tetris.views :as tetris.views]
   [games.puyo.views :as puyo.views]
   [games.subs :as subs]
   [games.select.views :as select.views]
   [re-frame.core :as rf]))

(defn root []
  (let [game @(rf/subscribe [::subs/selected-game])]
    [:div#root
     {:style {:width "100vw"}}
     (case game
       :tetris [tetris.views/page]
       :puyo   [puyo.views/page]
       nil     [select.views/page])]))


