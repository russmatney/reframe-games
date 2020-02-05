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

(def initial-controls
  {:move-left  {:label "Move Left"
                :keys  (set ["left" "h" "a"])
                :event [:games.puyo.events/move-piece :left]}
   :move-down  {:label "Move Down"
                :keys  (set ["down" "j" "s"])
                :event [:games.puyo.events/move-piece :down]}
   :move-right {:label "Move Right"
                :keys  (set ["right" "l" "d"])
                :event [:games.puyo.events/move-piece :right]}
   :hold       {:label "Hold"
                :keys  (set ["space"])
                :event [:games.puyo.events/hold-and-swap-piece]}
   :rotate     {:label "Rotate Piece"
                :keys  (set ["up" "k" "w"])
                :event [:games.puyo.events/rotate-piece]}
   :pause      {:label "Pause"
                :keys  (set ["enter"])
                :event [:games.puyo.events/toggle-pause]}
   :controls   {:label "Controls"
                :keys  (set ["c"])
                :event [:games.puyo.events/set-view :controls]}
   :about      {:label "About"
                :keys  (set ["b"])
                :event [:games.puyo.events/set-view :about]}
   :game       {:label "Return to game"
                :keys  (set ["g"])
                :event [:games.puyo.events/set-view :game]}})

(def initial-db
  {:game-grid
   (grid/build-grid {:height       10
                     :width        8
                     :phantom-rows 2})
   ;; TODO move entry-cell into grid.
   :entry-cell     {:x 0 :y -1}
   :piece-queue    (repeat 5 entry-cell->puyo)
   :min-queue-size 5
   :group-size     4 ;; number of puyos in a group to be removed
   :tick-timeout   500
   :paused?        false
   :gameover?      false
   :controls       initial-controls})



