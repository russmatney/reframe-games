(ns games.debug.controls
  (:require [games.controls.core :as controls]))

(defn initial
  [game-opts]
  [{:id    (controls/->id ::move-left game-opts)
    :label "Move Left"
    :keys  (set ["left" "h" "a"])
    ;; :event [:games.controls.events/move-piece game-opts :left]
    :event [:games.debug.events/instant-fall game-opts :left]}
   {:id    (controls/->id ::move-down game-opts)
    :label "Move Down"
    :keys  (set ["down" "j" "s"])
    ;; :event [:games.controls.events/move-piece game-opts :down]
    :event [:games.debug.events/instant-fall game-opts :down]}
   {:id    (controls/->id ::move-right game-opts)
    :label "Move Right"
    :keys  (set ["right" "l" "d"])
    :event [:games.debug.events/move-piece game-opts :right]}
   {:id    (controls/->id ::move-up game-opts)
    :label "Move Up"
    :keys  (set ["up" "k" "s"])
    :event [:games.debug.events/move-piece game-opts :up]}
   {:id    (controls/->id ::rotate game-opts)
    :label "Rotate"
    :keys  (set ["space"])
    :event [:games.debug.events/rotate-piece game-opts]}])
