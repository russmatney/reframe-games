(ns games.puyo.controls
  (:require [games.controls.core :as controls]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initial Controls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn initial
  [game-opts]
  [{:id    (controls/->id ::move-left game-opts)
    :label "Move Left"
    :keys  (set ["left" "h" "a"])
    :event [:games.puyo.events/move-piece game-opts :left]}
   {:id    (controls/->id ::move-down game-opts)
    :label "Move Down"
    :keys  (set ["down" "j" "s"])
    :event [:games.puyo.events/instant-fall game-opts :down]
    ;; :event [:games.puyo.events/move-piece game-opts :down]
    }
   {:id    (controls/->id ::move-right game-opts)
    :label "Move Right"
    :keys  (set ["right" "l" "d"])
    :event [:games.puyo.events/move-piece game-opts :right]}
   {:id    (controls/->id ::hold game-opts)
    :label "Hold"
    :keys  (set ["space"])
    :event [:games.puyo.events/hold-and-swap-piece game-opts]}
   {:id    (controls/->id ::rotate game-opts)
    :label "Rotate"
    :keys  (set ["up" "k" "w"])
    :event [:games.puyo.events/rotate-piece game-opts]}
   {:id    (controls/->id ::pause game-opts)
    :label "Pause"
    :keys  (set ["enter"])
    :event [:games.puyo.events/toggle-pause game-opts]}])
