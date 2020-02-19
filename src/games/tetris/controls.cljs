(ns games.tetris.controls
  (:require [games.controls.core :as controls]))

(defn initial
  [game-opts]
  [{:id    (controls/->id ::move-left game-opts)
    :label "Move Left"
    :keys  (set ["left" "h" "a"])
    :event [:games.tetris.events/move-piece game-opts :left]}
   {:id    (controls/->id ::move-down game-opts)
    :label "Move Down"
    :keys  (set ["down" "j" "s"])
    ;; :event [:games.tetris.events/move-piece game-opts :down]
    :event [:games.tetris.events/instant-fall game-opts :down]}
   {:id    (controls/->id ::move-right game-opts)
    :label "Move Right"
    :keys  (set ["right" "l" "d"])
    :event [:games.tetris.events/move-piece game-opts :right]}
   {:id    (controls/->id ::hold game-opts)
    :label "Hold"
    :keys  (set ["space"])
    :event [:games.tetris.events/hold-and-swap-piece game-opts]}
   {:id    (controls/->id ::rotate game-opts)
    :label "Rotate"
    :keys  (set ["up" "k" "w"])
    :event [:games.tetris.events/rotate-piece game-opts]}
   {:id    (controls/->id ::pause game-opts)
    :label "Pause"
    :keys  (set ["enter"])
    :event [:games.tetris.events/toggle-pause game-opts]}])
