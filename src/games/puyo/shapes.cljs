(ns games.puyo.shapes)


(defn build-piece-fn [colors]
  (let [[colorA colorB] colors]
    (fn [{x :x y :y}]
      [{:x x :y y :anchor? true :color colorA}
       {:x x :y (- y 1) :color colorB}])))

(defn next-bag
  "'bag' terminology carried over from tetris."
  [{:keys [game-opts min-queue-size]}]
  (let [colors (:colors game-opts)]
    (repeatedly min-queue-size
                (fn []
                  [(rand-nth colors) (rand-nth colors)]))))

(comment
  (let [colors [:red :blue :green]]
    (repeatedly 4
                (fn []
                  [(rand-nth colors) (rand-nth colors)]))))
