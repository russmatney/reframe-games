(ns games.grid.core-test
  (:require
   [games.grid.core :as sut]
   [cljs.test :as t :refer-macros [deftest is testing]]))

;; TODO move somewhere relevant
(enable-console-print!)

(deftest build-grid-test
  (testing "builds a grid"
    (let [width 3 height 5
          gdb   (sut/build-grid
                  {:width width :height height})]
      (testing "with the expected width"
        (doall
          (map
            (fn [row]
              (is (= (count row) width)))
            (:grid gdb))))

      (testing "with the expected height"
        (is (= (count (:grid gdb)) height)))

      (testing "sets x and y on cells"
        (testing "first cell"
          (is (= (-> gdb :grid first first)
                 {:x 0
                  :y 0})))
        (testing "one corner"
          (is (= (-> gdb :grid reverse first first)
                 {:x 0
                  :y (- height 1)})))
        (testing "another corner"
          (is (= (-> gdb :grid first reverse first)
                 {:x (- width 1)
                  :y 0})))
        (testing "last cell"
          (is (= (-> gdb :grid reverse first reverse first)
                 {:x (- width 1)
                  :y (- height 1)})))))))

(deftest build-grid-phantom-test
  (testing "builds a grid with phantom rows"
    (let [width           3 height 5
          phantom-rows    3
          phantom-columns 1
          gdb             (sut/build-grid
                            {:phantom-rows    phantom-rows
                             :phantom-columns phantom-columns
                             :width           width
                             :height          height})]
      (testing "with the expected width"
        (doall
          (map
            (fn [row]
              (is (= (count row) (+ phantom-columns width))))
            (:grid gdb))))
      (testing "with the expected height"
        (is (= (count (:grid gdb)) (+ phantom-rows height))))
      (testing "sets x and y on cells"
        (testing "first cell"
          (is (= (-> gdb :grid first first)
                 {:x (* -1 phantom-columns)
                  :y (* -1 phantom-rows)})))
        (testing "one corner"
          (is (= (-> gdb :grid reverse first first)
                 {:x (* -1 phantom-columns)
                  :y (- height 1)})))
        (testing "another corner"
          (is (= (-> gdb :grid first reverse first)
                 {:x (- width 1)
                  :y (* -1 phantom-rows)})))
        (testing "last cell"
          (is (= (-> gdb :grid reverse first reverse first)
                 {:x (- width 1)
                  :y (- height 1)})))))))


(deftest update-cells-test
  (testing "updates cells if pred is true"
    (testing "always true sets for all"
      (let [gdb (sut/build-grid {:height 5 :width 3})
            gdb (sut/update-cells
                  gdb (fn [_] true) #(assoc % :test true))]
        (doall
          (map
            (fn [r]
              (doall (map (fn [c] (is (:test c))) r)))
            (:grid gdb)))))
    (testing "sets for only those specified"
      (let [gdb          (sut/build-grid {:height 5 :width 3})
            target       {:x 2 :y 1}
            gdb          (sut/update-cells
                           gdb (fn [c] (and
                                         (= (:x c) (:x target))
                                         (= (:y c) (:y target))))
                           #(assoc % :test true))
            target-cell  (sut/get-cell gdb target)
            another-cell (sut/get-cell gdb (update target :y dec))
            ]
        (is (not (:test another-cell)))
        (is (:test target-cell))))))

(deftest add-cells-test
  (let [todo true]
    (is todo)))

(deftest move-cells-test
  (let [todo true]
    (is todo)))

(deftest move-cells-rotate-test
  (let [todo true]
    (is todo)))

(deftest cell-in-group-test
  (testing "returns true if a cell's coords are in the group"
    (let [group [{:x 1 :y 1 :blah 1} {:x 2 :y 3 :gibber true}]]
      (is (sut/cell-in-group? group {:x 1 :y 1}))
      (is (not (sut/cell-in-group? group {:x 0 :y 1}))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Instant Drop test
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn instant-fall-test
  "Creates and updates a grid db with the passed lists of cells.
  Calls `instant-fall` in the passed `direction`.

  Asserts that `empty-cells` are not :falling.
  Asserts that the `expected-cells` are :falling.
  "
  [{:keys
    [mark-cells expected-cells empty-cells move-cells
     keep-shape? direction print?]}]
  (let [direction (or direction :down)
        move-opts {:cells       move-cells
                   :keep-shape? keep-shape?
                   :direction   direction
                   :can-move?   #(not (:falling %))}
        gdb
        (-> (sut/build-grid {:height 5 :width 3 :phantom-rows 2})
            (sut/update-cells
              #(sut/cell-in-group? mark-cells %)
              #(assoc % :falling true))
            (sut/instant-fall move-opts))]
    (doall
      (map
        (fn [cell]
          (let [c (sut/get-cell gdb cell)]
            (when print?
              (println "should be falling")
              (println c))
            (is (:falling c))))
        expected-cells))
    (doall
      (map
        (fn [cell]
          (let [c (sut/get-cell gdb cell)]
            (when print?
              (println "should not be falling")
              (println c))
            (is (not (:falling c)))))
        empty-cells))))

(deftest instant-fall-test-basic
  (testing "keep-shape?"
    (instant-fall-test
      {:mark-cells     [{:x 1 :y 0}]
       :move-cells     [{:x 1 :y 0}]
       :empty-cells    [{:x 1 :y 0}]
       :expected-cells [{:x 1 :y 4}]
       :print?         false
       :keep-shape?    true}))
  (testing "drop-shape?"
    (instant-fall-test
      {:mark-cells     [{:x 1 :y 0}]
       :move-cells     [{:x 1 :y 0}]
       :empty-cells    [{:x 1 :y 0}]
       :expected-cells [{:x 1 :y 4}]
       :keep-shape?    false})))

(deftest instant-fall-test-blocked
  (testing "keep-shape?"
    (instant-fall-test
      {:mark-cells     [{:x 1 :y 0} {:x 1 :y 4}]
       :move-cells     [{:x 1 :y 0}]
       :empty-cells    [{:x 1 :y 0}]
       :expected-cells [{:x 1 :y 3} {:x 1 :y 4}]
       :keep-shape?    true}))
  (instant-fall-test
    {:mark-cells     [{:x 1 :y 0} {:x 1 :y 4} {:x 1 :y 3} {:x 1 :y 2} {:x 1 :y 1}]
     :move-cells     [{:x 1 :y 0}]
     :empty-cells    []
     :expected-cells [{:x 1 :y 0} {:x 1 :y 4} {:x 1 :y 3} {:x 1 :y 2} {:x 1 :y 1}]
     :keep-shape?    true})
  (testing "drop shape"
    (instant-fall-test
      {:mark-cells     [{:x 1 :y 0} {:x 1 :y 4}]
       :move-cells     [{:x 1 :y 0}]
       :empty-cells    [{:x 1 :y 0}]
       :expected-cells [{:x 1 :y 3} {:x 1 :y 4}]
       :keep-shape?    false})))
