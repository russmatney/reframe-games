(ns games.grid.core-test
  (:require
   [games.grid.core :as sut]
   [cljs.test :as t :refer-macros [deftest is testing]]
   [adzerk.cljs-console :as log]))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Misc cell utils test
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest get-cell-in-group-test
  (testing "returns the matching cell from a group with coords"
    (let [group [{:x 1 :y 1 :blah 1} {:x 2 :y 3 :gibber true}]]
      (is (= {:x 1 :y 1 :blah 1} (sut/get-cell-in-group group {:x 1 :y 1})))
      (is (= nil (sut/get-cell-in-group group {:x 0 :y 1}))))))

(deftest cell-in-group-test
  (testing "returns true if a cell's coords are in the group"
    (let [group [{:x 1 :y 1 :blah 1} {:x 2 :y 3 :gibber true}]]
      (is (sut/cell-in-group? group {:x 1 :y 1}))
      (is (not (sut/cell-in-group? group {:x 0 :y 1}))))))

(deftest add-cells-test
  (let [todo true]
    (is todo)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Move cells tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn move-cells-test
  "Creates and updates a grid db with the passed lists of cells.
  Calls `move-cells` in the passed `direction`.

  Asserts that `empty-cells` are not :flagged.
  Asserts that the `expected-cells` are :flagged.
  "
  [{:keys
    [mark-cells expected-cells empty-cells move-cells
     direction debug? force-fail?]}]
  (let [direction (or direction :down)

        move-opts {:cells     move-cells
                   :direction direction
                   :can-move? #(not (:flagged %))}
        gdb
        (-> (sut/build-grid {:height 5 :width 3 :phantom-rows 2})
            (sut/update-cells
              #(sut/cell-in-group? mark-cells %)
              (fn [c]
                (let [props (sut/get-cell-in-group mark-cells c)
                      props (dissoc props :x :y)]
                  (-> c
                      (assoc :flagged true)
                      (merge props))))))
        gdb'      (sut/move-cells gdb move-opts)]
    (when debug? (sut/log gdb))
    (when debug? (sut/log gdb'))
    (doall
      (map
        (fn [expected-cell]
          (let [c (sut/get-cell gdb' expected-cell)]
            (when (and debug? (not (:flagged c)))
              (log/debug "Should be flagged ~{c}"))
            (is (= expected-cell c))))
        (map #(assoc % :flagged true) expected-cells)))
    (doall
      (map
        (fn [cell]
          (let [c (sut/get-cell gdb' cell)]
            (when (and debug? (:flagged c))
              (log/debug "Should not be flagged ~{c}"))
            (is (not (:flagged c)))))
        empty-cells))
    (when force-fail? (is false))))

(deftest move-cells-basic
  (testing "move one cell down"
    (let [cells [{:x 1 :y 0}]]
      (move-cells-test
        {:mark-cells     cells
         :move-cells     cells
         :empty-cells    cells
         :expected-cells [{:x 1 :y 1}]
         :direction      :down}))))

(deftest move-cells-shape
  (testing "move shape down"
    (let [cells          [{:x 2 :y -1} {:x 2 :y -2} {:x 1 :y -1} {:x 1 :y 0}]
          expected-cells [{:x 2 :y 0} {:x 2 :y -1} {:x 1 :y 0} {:x 1 :y 1}]]
      (move-cells-test
        {:mark-cells     cells
         :move-cells     cells
         :empty-cells    [{:x 1 :y -1} {:x 2 :y -2}]
         :expected-cells expected-cells
         :direction      :down}))))

(deftest move-cells-shape-copy-order-down
  (testing "move shape copy order check - down"
    (let [cells          [{:x 2 :y -1 :a true}
                          {:x 2 :y -2 :b true}
                          {:x 1 :y -1 :c true}
                          {:x 1 :y 0 :d true}]
          expected-cells [{:x 2 :y 0 :a true}
                          {:x 2 :y -1 :b true}
                          {:x 1 :y 0 :c true}
                          {:x 1 :y 1 :d true}]]
      (move-cells-test
        {:mark-cells     cells
         :move-cells     cells
         :empty-cells    [{:x 1 :y -1} {:x 2 :y -2}]
         :expected-cells expected-cells
         :direction      :down}))))

(deftest move-cells-shape-copy-order-up
  (testing "move shape copy order check - up"
    (let [cells          [{:x 2 :y 0 :a true}
                          {:x 2 :y -1 :b true}
                          {:x 1 :y 0 :c true}
                          {:x 1 :y 1 :d true}]
          expected-cells [{:x 2 :y -1 :a true}
                          {:x 2 :y -2 :b true}
                          {:x 1 :y -1 :c true}
                          {:x 1 :y 0 :d true}]]
      (move-cells-test
        {:mark-cells     cells
         :move-cells     cells
         :empty-cells    [{:x 1 :y 1} {:x 2 :y 0}]
         :expected-cells expected-cells
         :direction      :up}))))

(deftest move-cells-shape-copy-order-left
  (testing "move shape copy order check - left"
    (let [cells
          [{:x 2 :y 0 :a true}
           {:x 2 :y -1 :b true}
           {:x 1 :y 0 :c true}
           {:x 1 :y 1 :d true}]
          expected-cells
          [{:x 1 :y 0 :a true}
           {:x 1 :y -1 :b true}
           {:x 0 :y 0 :c true}
           {:x 0 :y 1 :d true}]]
      (move-cells-test
        {:mark-cells     cells
         :move-cells     cells
         :empty-cells    [{:x 2 :y 0}
                          {:x 2 :y -1}]
         :expected-cells expected-cells
         :direction      :left}))))

(deftest move-cells-shape-copy-order-right
  (testing "move shape copy order check - right"
    (let [cells
          [{:x 1 :y 0 :a true}
           {:x 1 :y -1 :b true}
           {:x 0 :y 0 :c true}
           {:x 0 :y 1 :d true}]
          expected-cells
          [{:x 2 :y 0 :a true}
           {:x 2 :y -1 :b true}
           {:x 1 :y 0 :c true}
           {:x 1 :y 1 :d true}]]
      (move-cells-test
        {:mark-cells     cells
         :move-cells     cells
         :empty-cells    [{:x 0 :y 0}
                          {:x 0 :y 1}]
         :expected-cells expected-cells
         :direction      :right}))))

(deftest move-cells-rotate-test
  (let [todo true]
    (is todo)))

(deftest move-cells-rotate-near-wall-test
  (let [todo true]
    (is todo)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Instant Drop tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn instant-fall-test
  "Creates and updates a grid db with the passed lists of cells.
  Calls `instant-fall` in the passed `direction`.

  Asserts that `empty-cells` are not :flagged.
  Asserts that the `expected-cells` are :flagged.
  "
  [{:keys
    [mark-cells expected-cells empty-cells move-cells
     keep-shape? direction debug? force-fail?]}]
  (let [direction (or direction :down)
        move-opts {:cells       move-cells
                   :keep-shape? keep-shape?
                   :direction   direction
                   :can-move?   #(not (:flagged %))}
        gdb
        (-> (sut/build-grid {:height 5 :width 3 :phantom-rows 2})
            (sut/update-cells
              #(sut/cell-in-group? mark-cells %)
              #(assoc % :flagged true)))
        gdb'      (sut/instant-fall gdb move-opts)]
    (when debug? (sut/log gdb))
    (when debug? (sut/log gdb'))
    (doall
      (map
        (fn [cell]
          (let [c (sut/get-cell gdb' cell)]
            (when (and debug? (not (:flagged c)))
              (log/debug "Should be flagged ~{c}"))
            (is (:flagged c))))
        expected-cells))
    (doall
      (map
        (fn [cell]
          (let [c (sut/get-cell gdb' cell)]
            (when (and debug? (:flagged c))
              (log/debug "Should not be flagged ~{c}"))
            (is (not (:flagged c)))))
        empty-cells))
    (when force-fail? (is false))))

(deftest instant-fall-test-basic
  (testing "single cell drop"
    (testing "keep-shape"
      (instant-fall-test
        {:mark-cells     [{:x 1 :y 0}]
         :move-cells     [{:x 1 :y 0}]
         :empty-cells    [{:x 1 :y 0}]
         :expected-cells [{:x 1 :y 4}]
         :keep-shape?    true}))
    (testing "don't keep-shape"
      (instant-fall-test
        {:mark-cells     [{:x 1 :y 0}]
         :move-cells     [{:x 1 :y 0}]
         :empty-cells    [{:x 1 :y 0}]
         :expected-cells [{:x 1 :y 4}]
         :keep-shape?    false}))))

(deftest instant-fall-test-blocked
  (testing "single cell drop with blockers"
    (testing "keep-shape"
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
    (testing "don't keep shape"
      (instant-fall-test
        {:mark-cells     [{:x 1 :y 0} {:x 1 :y 4}]
         :move-cells     [{:x 1 :y 0}]
         :empty-cells    [{:x 1 :y 0}]
         :expected-cells [{:x 1 :y 3} {:x 1 :y 4}]
         :keep-shape?    false}))))

(deftest instant-fall-test-stacked-piece
  (testing "vertical two-cell drop"
    (testing "keep-shape?"
      (instant-fall-test
        {:mark-cells     [{:x 1 :y 0} {:x 1 :y 1}]
         :move-cells     [{:x 1 :y 0} {:x 1 :y 1}]
         :empty-cells    [{:x 1 :y 0} {:x 1 :y 1}]
         :expected-cells [{:x 1 :y 3} {:x 1 :y 4}]
         :keep-shape?    true}))
    (testing "don't keep shape"
      (instant-fall-test
        {:mark-cells     [{:x 1 :y 0} {:x 1 :y 1}]
         :move-cells     [{:x 1 :y 0} {:x 1 :y 1}]
         :empty-cells    [{:x 1 :y 0} {:x 1 :y 1}]
         :expected-cells [{:x 1 :y 3} {:x 1 :y 4}]
         :keep-shape?    false}))))

(deftest instant-fall-test-staggered-piece-keep-shape
  (testing "horizontal shape drop, keep shape"
    (let [shape            (map #(sut/relative {:x 1 :y 0} %)
                                [{:x 1 :y -1} {:y -1} {} {:x -1}])
          shape-after-drop (map #(sut/relative {:x 1 :y 4} %)
                                [{:x 1 :y -1} {:y -1} {} {:x -1}])]
      (instant-fall-test
        {:mark-cells     shape
         :move-cells     shape
         :empty-cells    shape
         :expected-cells shape-after-drop
         :keep-shape?    true}))))

(deftest instant-fall-test-staggered-piece-drop-shape
  (testing "horizontal shape drop, don't keep shape"
    (let [shape            (map #(sut/relative {:x 1 :y 0} %)
                                [{:x 1 :y -1} {:y -1} {} {:x -1}])
          shape-after-drop (map #(sut/relative {:x 1 :y 4} %)
                                [{:x 1 :y 0} {:y -1} {} {:x -1}])]
      (instant-fall-test
        {:mark-cells     shape
         :move-cells     shape
         :empty-cells    shape
         :expected-cells shape-after-drop
         :keep-shape?    false}))))

(deftest instant-fall-test-staggered-piece-keep-shape-vertical
  (testing "vertical shape drop, keep shape"
    (let [shape            [{:x 2 :y -1} {:x 2 :y -2} {:x 1 :y -1} {:x 1 :y 0}]
          shape-after-drop [{:x 2 :y 2} {:x 2 :y 3} {:x 1 :y 3} {:x 1 :y 4}]
          other-cells      [{:x 2 :y 4}]]
      (instant-fall-test
        {:mark-cells     (concat shape other-cells)
         :move-cells     shape
         :empty-cells    shape
         :expected-cells (concat shape-after-drop other-cells)
         :keep-shape?    true}))))

(deftest instant-fall-test-staggered-piece-keep-shape-vertical-blocked
  (testing "vertical shape drop, keep shape, more blockers"
    (let [shape            [{:x 2 :y -1} {:x 2 :y -2} {:x 1 :y -1} {:x 1 :y 0}]
          shape-after-drop [{:x 2 :y 2} {:x 2 :y 1} {:x 1 :y 3} {:x 1 :y 2}]
          blockers         [{:x 2 :y 4} {:x 1 :y 4}]
          empty            [{:x 2 :y 3}]]
      (instant-fall-test
        {:mark-cells     (concat shape blockers)
         :move-cells     shape
         :empty-cells    (concat shape empty)
         :expected-cells (concat shape-after-drop blockers)
         :keep-shape?    true}))))

(deftest instant-fall-test-move-one-cell-keep-shape
  (testing "vertical shape drop, keep shape, blocker one below"
    (let [shape            [{:x 2 :y -1} {:x 2 :y -2} {:x 1 :y -1} {:x 1 :y 0}]
          empty            [{:x 1 :y -1} {:x 2 :y -2}]
          shape-after-drop [{:x 2 :y 0} {:x 2 :y -1} {:x 1 :y 0} {:x 1 :y 1}]
          blockers         [{:x 1 :y 2}]]
      (instant-fall-test
        {:mark-cells     (concat shape blockers)
         :move-cells     shape
         :empty-cells    empty
         :expected-cells (concat shape-after-drop blockers)
         :debug?         true
         :keep-shape?    true}))))

(deftest instant-fall-test-move-one-cell-no-keep-shape
  (testing "vertical shape drop, don't keep shape, blocker one below"
    (let [shape            [{:x 2 :y -1} {:x 2 :y -2} {:x 1 :y -1} {:x 1 :y 0}]
          shape-after-drop [{:x 2 :y 3} {:x 2 :y 4} {:x 1 :y 0} {:x 1 :y 1}]
          empty            [{:x 2 :y -1} {:x 2 :y -2} {:x 1 :y -1}]
          blockers         [{:x 1 :y 2}]]
      (instant-fall-test
        {:mark-cells     (concat shape blockers)
         :move-cells     shape
         :empty-cells    empty
         :expected-cells (concat shape-after-drop blockers)
         :keep-shape?    false}))))

