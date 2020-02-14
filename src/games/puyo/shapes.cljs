(ns games.puyo.shapes)


(defn build-piece-fn [game-opts]
  (let [colors (:colors game-opts)
        colorA (rand-nth colors)
        colorB (rand-nth colors)]
    (fn [{x :x y :y}]
      [{:x x :y y :anchor? true :color colorA}
       {:x x :y (- y 1) :color colorB}])))

(defn next-bag
  "'bag' terminology carried over from tetris."
  [_]
  (repeat 5 build-piece-fn))
