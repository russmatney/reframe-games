(ns games.tetris.db
  (:require
   [games.grid.core :as grid]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Piece Shapes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def shapes
  [{:k :square :cells [{:x 1 :y -1} {:x 1} {:y -1} {}]}
   {:k :line :cells [{:x 2} {:x 1} {:anchor true} {:x -1}]}
   {:k :t :cells [{:x -1} {:x 1} {} {:y -1}]}
   {:k :z :cells [{:x -1 :y -1} {:x 1} {:anchor true} {:x -1}]}
   {:k :s :cells [{:x 1 :y -1} {:y -1} {:anchor true} {:x -1}]}
   {:k :r :cells [{:x -1 :y -1} {:y -1} {:anchor true} {:x -1}]}
   {:k :l :cells [{:x -1 :y -1} {:x 1} {:anchor true} {:y -1}]}])

(def shapes-map
  "The above `shapes` as a map by its `:key`"
  (into {} (map (fn [{k :k :as s}] [k s]) shapes)))

(defn cell->props [shape]
  (case (:k shape)
    :line   {:color :light-blue :type :line}
    :square {:color :yellow :type :square}
    :l      {:color :orange :type :l}
    :r      {:color :blue :type :r}
    :s      {:color :red :type :s}
    :z      {:color :red :type :z}
    :t      {:aka   #{"mr. t"}
             :color :magenta
             :type  :t}))

;; TODO move to tetris views
(defn cell->style [shape]
  (case (:k shape)
    :line   {:background "#6EBFF5"}
    :square {:background "rgb(247,213,29)"}
    :l      {:background "rgb(146,204,65)"}
    :r      {:background "#209CEE"}
    :s      {:background "#FE493C"}
    :z      {:background "rgb(231,110,85)"}
    :t      {:background "#B564D4"}
    ;; TODO add 'log' function
    nil     (println "shape missing" shape)))

(defn shape->ec->cell
  [{:keys [cells] :as shape}]
  (let [style (cell->style shape)
        props (cell->props shape)]
    (fn [ec]
      (map (comp
             #(assoc % :props props)
             #(assoc % :style style)
             #(grid/relative ec %))
           cells))))

(defn single-cell-shape [entry-cell]
  [entry-cell])

(def allowed-shape-fns
  (map shape->ec->cell shapes))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initial DB
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn initial-controls
  [game-opts]
  {:move-left  {:label "Move Left"
                :keys  (set ["left" "h" "a"])
                :event [:games.tetris.events/move-piece game-opts :left]}
   :move-down  {:label "Move Down"
                :keys  (set ["down" "j" "s"])
                :event [:games.tetris.events/move-piece game-opts :down]}
   :move-right {:label "Move Right"
                :keys  (set ["right" "l" "d"])
                :event [:games.tetris.events/move-piece game-opts :right]}
   :hold       {:label "Hold"
                :keys  (set ["space"])
                :event [:games.tetris.events/hold-and-swap-piece game-opts]}
   :rotate     {:label "Rotate"
                :keys  (set ["up" "k" "w"])
                :event [:games.tetris.events/rotate-piece game-opts]}
   ;; TODO update this event to the global one?
   :pause      {:label "Pause"
                :keys  (set ["enter"])
                :event [:games.tetris.events/toggle-pause game-opts]}})

(def piece-grid (grid/build-grid {:height     2
                                  :width      4
                                  :entry-cell {:x 1 :y 1}}))

(def defaults
  {:step-timeout    500
   :ignore-controls false})

(defn initial-db
  "Creates an initial tetris game-state."
  ([] (initial-db {:name :default}))

  ([game-opts]
   (let [{:keys [name game-grid step-timeout ignore-controls] :as game-opts}
         (merge defaults game-opts)]
     {:name      name
      :game-opts game-opts

      ;; game matrix
      :game-grid
      (grid/build-grid
        (merge
          {:height       10
           :width        10
           :phantom-rows 2
           :entry-cell   {:x 5 :y -1}}
          game-grid))

      ;; game logic
      :step-timeout step-timeout
      :paused?      false
      :gameover?    false

      ;; queue
      :piece-queue       (shuffle allowed-shape-fns)
      :min-queue-size    5
      :allowed-shape-fns allowed-shape-fns
      :preview-grids     (repeat 3 piece-grid)

      ;; controls
      :controls        (initial-controls game-opts)
      :ignore-controls ignore-controls

      ;; hold/swap
      :falling-shape-fn nil
      :held-shape-fn    nil
      :held-grid        piece-grid
      :hold-lock        false

      ;; timer
      :time 0

      ;; level/score
      :level                1
      :rows-per-level       5
      :rows-cleared         0
      :pieces-played        0
      :score                0
      :score-per-row-clear  10
      :rows-in-combo        0
      :last-combo-piece-num nil
      })))

