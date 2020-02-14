(ns games.tetris.shapes
  (:require [games.grid.core :as grid]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Piece Shapes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def tetrominos
  [{:k :square :cells [{:x 1 :y -1} {:x 1} {:y -1} {:anchor? true}]}
   {:k :line :cells [{:x 2} {:x 1} {:anchor? true} {:x -1}]}
   {:k :t :cells [{:x -1} {:x 1} {:anchor? true} {:y -1}]}
   {:k :z :cells [{:x -1 :y -1} {:y -1} {:anchor? true} {:x 1}]}
   {:k :s :cells [{:x 1 :y -1} {:y -1} {:anchor? true} {:x -1}]}
   {:k :r :cells [{:x -1 :y -1} {:x 1} {:anchor? true} {:x -1}]}
   {:k :l :cells [{:x 1 :y -1} {:x 1} {:anchor? true} {:x -1}]}])

(def tetrominos-map
  "The above `tetrominos` as a map by its `:key`"
  (into {} (map (fn [{k :k :as s}] [k s]) tetrominos)))

(defn cell->props [shape]
  (case (:k shape)
    :square {:color :yellow}
    :line   {:color :light-blue}
    :l      {:color :orange}
    :r      {:color :blue}
    :s      {:color :green}
    :z      {:color :red}
    :t      {:color :magenta}))

(defn shape->ec->cell
  [{:keys [cells] :as shape}]
  (let [props (cell->props shape)]
    (fn [ec]
      (map (comp
             #(merge % props)
             #(grid/relative ec %))
           cells))))

(defn single-cell-shape [entry-cell]
  [entry-cell])

(def allowed-shape-fns
  (map shape->ec->cell tetrominos))

(defn next-bag
  "Returns a shuffled group of the allowed shapes.
  https://tetris.wiki/Random_Generator
  "
  [{:keys [allowed-shape-fns]}]
  (shuffle allowed-shape-fns))
