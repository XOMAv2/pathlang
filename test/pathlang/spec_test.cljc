(ns pathlang.spec-test
  (:require #?(:clj [clojure.test :refer [deftest testing is are run-tests]]
               :cljs [cljs.test :refer-macros [deftest testing is are run-tests]])
            #?(:clj [clojure.spec.alpha :as s]
               :cljs [cljs.spec.alpha :as s])
            [pathlang.spec :as pls]))

(deftest pl-context-test
  (are [x y] (= x (s/valid? ::pls/context y))
    true  {'kek/oleg (fn [a b])
           'kek/egor (fn [a])
           '$ 5}
    false {'oleg (fn [a b])}
    false {'kek/egor 5}
    true  {'$ {:a :b}}))

(deftest pl-expression-test
  (are [x y] (= x (s/valid? ::pls/expression y))
    false 'kek
    true  '({:a 1})
    false '(kek (lol) (1 2 ([3]) 4))
    true  '(kek (lol) (1 2 ((3)) 4))))

#?(:cljs (run-tests))
