(ns limp.core-test
  (:require [clojure.test :refer :all]
            [limp.core :as sut]))

(deftest adds-numbers
  (testing "sanity"
    (is (= 2 (+ 1 1)))))
