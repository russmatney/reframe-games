(ns games.puyo.db
  (:require
   [games.grid.core :as grid]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Piece Types
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def colors
  [:red
   :green
   :yellow
   :blue])

(defn assign-color [c]
  (merge c {:color (rand-nth colors)}))

(defn entry-cell->puyo [{x :x y :y}]
  (let [cells [{:x x :y y :anchor true}
               {:x x :y (- y 1)}]]
    (map assign-color cells)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initial DB
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def shape-opts {})

(def initial-db
  {:game-grid
   (grid/build-grid {:height 8
                     :width 4
                     :phantom-rows 2})
   ;; TODO move entry-cell into grid.
   :entry-cell {:x 1 :y -1}
   :piece-queue (repeat 5 entry-cell->puyo)
   :min-queue-size 5
   :group-size 4 ;; number of puyos in a group to be removed
   :tick-timeout 500
   :paused? false
   :gameover? false
   :controls {:move-left (set ["left" "h" "a"])
              :move-down (set ["down" "j" "s"])
              :move-right (set ["right" "l" "d"])
              :hold (set ["space"])
              :rotate (set ["up" "k" "w"])
              :pause (set ["enter"])
              :controls (set ["c"])
              :about (set ["b"])
              :game (set ["g"])}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Control helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO move into game.controls namespace and de-dupe
(def key-label->re-pressed-key
  "Maps a nice string to a re-pressed key with keyCode."
  {"enter" {:keyCode 13}
   "space" {:keyCode 32}
   "left" {:keyCode 37}
   "right" {:keyCode 39}
   "up" {:keyCode 38}
   "down" {:keyCode 40}
   "a" {:keyCode 65}
   "b" {:keyCode 66}
   "c" {:keyCode 67}
   "d" {:keyCode 68}
   "e" {:keyCode 69}
   "f" {:keyCode 70}
   "g" {:keyCode 71}
   "h" {:keyCode 72}
   "j" {:keyCode 74}
   "k" {:keyCode 75}
   "l" {:keyCode 76}
   "s" {:keyCode 83}
   "w" {:keyCode 87}})

;; TODO move into game.controls namespace and de-dupe
(def supported-keys (set (keys key-label->re-pressed-key)))

;; TODO move into game.controls namespace and de-dupe
(def control->label
  "Maps a control to a human label"
  {:move-left "Move Left"
   :move-right "Move Right"
   :move-down "Move Down"
   :rotate "Rotate"
   :hold "Hold / Swap"
   :pause "Pause"
   :controls "Controls"
   :about "About"
   :game "Back to Game"})
