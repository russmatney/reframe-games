(ns games.tetris.shapes
  (:require [games.grid.core :as grid]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Piece Shapes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def tetrominos
  [{:type :square :cells [{:x 1 :y -1} {:x 1} {:y -1} {:anchor? true}]}
   {:type :line :cells [{:x 2} {:x 1} {:anchor? true} {:x -1}]}
   {:type :t :cells [{:x -1} {:x 1} {:anchor? true} {:y -1}]}
   {:type :z :cells [{:x -1 :y -1} {:y -1} {:anchor? true} {:x 1}]}
   {:type :s :cells [{:x 1 :y -1} {:y -1} {:anchor? true} {:x -1}]}
   {:type :r :cells [{:x -1 :y -1} {:x 1} {:anchor? true} {:x -1}]}
   {:type :l :cells [{:x 1 :y -1} {:x 1} {:anchor? true} {:x -1}]}])

(def type->cells
  "The above `tetrominos` as a map to cells by its `:type`"
  (into {} (map (fn [{type :type cells :cells}] [type cells]) tetrominos)))

(defn type->props [type]
  (case type
    :square {:color :yellow}
    :line   {:color :light-blue}
    :l      {:color :orange}
    :r      {:color :blue}
    :s      {:color :green}
    :z      {:color :red}
    :t      {:color :magenta}))

(defn type->ec->cell
  [type]
  (let [cells (get type->cells type)
        props (type->props type)]
    (fn [ec]
      (map (comp
             #(merge % props)
             #(grid/relative ec %))
           cells))))

(def allowed-shapes
  (map :type tetrominos))

(defn next-bag
  "Returns a shuffled group of the allowed shapes.
  https://tetris.wiki/Random_Generator
  "
  [{:keys [allowed-shapes]}]
  (shuffle allowed-shapes))
