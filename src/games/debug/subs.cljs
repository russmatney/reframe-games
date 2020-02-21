(ns games.debug.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  ::debug-game-opts
  (fn [db]
    (let [games     (vals (:games db))
          game-opts (map :game-opts games)]
      (filter :debug-game? game-opts))))
