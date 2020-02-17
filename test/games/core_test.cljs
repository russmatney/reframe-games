(ns games.core-test
  (:require
   [cljs.test :as t :refer-macros [deftest is testing]]))


(deftest fake-test
  (testing "fake description"
    (is (= 1 1))))
