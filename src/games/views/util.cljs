(ns games.views.util)

(defn with-precision [p num]
  (let [num (or num 0)]
    (.toFixed num p)))
