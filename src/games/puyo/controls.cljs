(ns games.puyo.controls)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initial Controls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn initial
  [game-opts]
  [{:id    :move-left
    :label "Move Left"
    :keys  (set ["left" "h" "a"])
    :event [:games.puyo.events/move-piece game-opts :left]}
   {:id    :move-down
    :label "Move Down"
    :keys  (set ["down" "j" "s"])
    :event [:games.puyo.events/instant-fall game-opts :down]
    ;; :event [:games.puyo.events/move-piece game-opts :down]
    }
   {:id    :move-right
    :label "Move Right"
    :keys  (set ["right" "l" "d"])
    :event [:games.puyo.events/move-piece game-opts :right]}
   {:id    :hold
    :label "Hold"
    :keys  (set ["space"])
    :event [:games.puyo.events/hold-and-swap-piece game-opts]}
   {:id    :rotate
    :label "Rotate"
    :keys  (set ["up" "k" "w"])
    :event [:games.puyo.events/rotate-piece game-opts]}
   {:id    :pause
    :label "Pause"
    :keys  (set ["enter"])
    :event [:games.puyo.events/toggle-pause game-opts]}])
