(ns pathlang.spec-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [pathlang.spec :as pls]))

(deftest pl-context-test
  (is (= true (s/valid? ::pls/context {'kek/oleg (fn [a b])
                                       'kek/egor (fn [a])
                                       '$ 5}))))

(deftest pl-expression-test
  (is (= false (s/valid? ::pls/expression 'kek)))
  (is (= true  (s/valid? ::pls/expression '({:a 1}))))
  (is (= false (s/valid? ::pls/expression '(kek (lol) (1 2 ([3]) 4)))))
  (is (= true  (s/valid? ::pls/expression '(kek (lol) (1 2 ((3)) 4))))))
