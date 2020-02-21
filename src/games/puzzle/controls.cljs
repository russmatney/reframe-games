(ns games.puzzle.controls
  (:require [games.controls.core :as controls]))

(defn initial
  [game-opts]
  [{:id    (controls/->id ::move-left game-opts)
    :label "Move Left"
    :keys  (set ["left" "h" "a"])
    :event [:games.puzzle.events/move-piece game-opts :left]}
   {:id    (controls/->id ::move-down game-opts)
    :label "Move Down"
    :keys  (set ["down" "j" "s"])
    :event [:games.puzzle.events/move-piece game-opts :down]}
   {:id    (controls/->id ::move-up game-opts)
    :label "Move Up"
    :keys  (set ["up" "j" "s"])
    :event [:games.puzzle.events/move-piece game-opts :up]}
   {:id    (controls/->id ::move-right game-opts)
    :label "Move Right"
    :keys  (set ["right" "l" "d"])
    :event [:games.puzzle.events/move-piece game-opts :right]}
   {:id    (controls/->id ::rotate game-opts)
    :label "Rotate"
    :keys  (set ["up" "k" "w"])
    :event [:games.puzzle.events/rotate-piece game-opts]}
   {:id    (controls/->id ::pause game-opts)
    :label "Pause"
    :keys  (set ["enter"])
    :event [:games.puzzle.events/toggle-pause game-opts]}])
