(ns games.core-test
  (:require
   [games.core :as sut]
   [cljs.test :as t :refer-macros [deftest is testing]]))


(deftest fake-test
  (testing "fake description"
    (is (= 1 1))))
