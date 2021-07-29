(ns pathlang.interpreter-test
  (:require #?(:clj [clojure.test :as test :refer [deftest testing is are run-tests]]
               :cljs [cljs.test :as test :refer-macros [deftest testing is are run-tests]])
            [pathlang.stdlib :as std]
            [pathlang.interpreter :as pl :refer [evaluate]]))

(deftest keyword-test
  (are [x y] (= (quote x) (evaluate (str (quote y))))
    (1 2 nil) (:foo {:foo 1} ({:foo 2} {:bar 1}) ())
    (1 2 4)   (:foo {:foo 1} ({:foo 2} {:foo 4}) ())
    ()        (:foo ())
    (1 2 3 4) (:foo {:foo (1 2 3)} {:foo (4)} {:foo ()})
    (1)       (:foo {:foo 1}))
  (are [x] (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                    (evaluate (str (quote x))))
    (:foo nil)
    (:foo ((1)))))

(deftest implicit-list-test
  (are [x y] (= (quote x) (evaluate (str (quote y))))
    (1 2)          (1 2)
    (1 2 3)        (1 2 (3))
    (1 "2" {:x 1}) (1 "2" {:x 1})
    ()             ())
  (are [x] (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                    (evaluate (str (quote x))))
    (())
    (1 2 (3 4) 5)))

(deftest value-test
  (are [x y] (= x (apply #'pl/pl-eval y))
    5          [5 {}]
    true       [true {}]
    nil        [nil {}]
    \newline   [\newline {}]
    "some str" ["some str" {}]
    ()         [() std/fns]
    :keyword   [:keyword {}]
    42         ['$ {'$ 42}])
  (are [x] (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                    x)
    (doall (#'pl/pl-eval ''q-symbol std/fns))
    (#'pl/pl-eval 'unq-symbol {})
    (evaluate (str 5))
    (evaluate "nil")))

(deftest hash-map-test
  (is (= {"xy" 3}
         (#'pl/pl-eval '{(+ "x" "y") (+ 1 $)} (merge std/fns {'$ 2})))))

(deftest list-test
  (are [x y] (= (quote x) (evaluate (str (quote y))))
    (1 2)          (list 1 2)
    (1 2 3)        (list 1 2 (3))
    (1 "2" {:x 1}) (list 1 "2" {:x 1})
    ()             (list))
  (are [x] (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                    (evaluate (str (quote x))))
    (list ())
    (1 2 (3 4) 5)))

(deftest if-test
  (are [x y] (= x (evaluate (str (quote y))))
    1  (if () (+ 1 2) (- 2 1))
    3  (if (= 1 1) (+ 1 2) (- 2 1))
    :b (if nil :a :b))
  (is (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
               (evaluate (str '(if (= 5 5) "success"))))))

(deftest count-test
  (are [x y] (= (quote x) (evaluate (str (quote y))))
    3 (count (1 2 3))
    3 (count 1 2 3)
    4 (count (1) (2 3) 4)
    5 (count () nil (1 2 3 nil))
    1 (count nil)
    0 (count ())
    3 (count (+ 1 2) (:foo {:foo 1} {:bar 2} ()))))

(deftest =-test
  (are [x y] (= x (evaluate (str (quote y))))
    false (= "red" ("green") "blue")
    true  (= "red" ("red") "red")
    #?@(:cljs [true (= 2.0 2)])
    false (= "red" nil))
  (are [x] (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                    (evaluate (str (quote x))))
    (= "red" ("red" "green"))
    (= "red" (1) {:x 1})
    (= "red" ((1)))
    (= "red")
    #?(:clj (= 2.0 2))
    (= "red" ())))

(deftest not=-test
  (are [x y] (= x (evaluate (str (quote y))))
    true  (not= "red" ("green") "blue")
    false (not= "red" ("red") "red")
    true  (not= "red" nil))
  (are [x] (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                    (evaluate (str (quote x))))
    (not= "red" ("red" "green"))
    (not= "red" (1) {:x 1})
    (not= "red" ((1)))
    (not= "red")
    (not= "red" ())))

(deftest or-test
  (are [x y] (= x (evaluate (str (quote y))))
    true  (or nil () false 1 (/ 1 0))
    false (or nil ())
    true  (or 1 (())))
  (are [x] (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                    (evaluate (str (quote x))))
    (or 1)
    (or (()) 1)))

(deftest and-test
  (are [x y] (= x (evaluate (str (quote y))))
    false (and () (/ 1 0))
    true  (and 1 "a" :a (now)))
  (are [x] (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                    (evaluate (str (quote x))))
    (and 1)
    (and 1 (()))))

(deftest not-test
  (are [x y] (= x (evaluate (str (quote y))))
    true  (not ())
    true  (not nil)
    true  (not false)
    false (not "string")
    false (not :a))
  (are [x] (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                    (evaluate (str (quote x))))
    (not :a :b)
    (not (()))))

(deftest >-test
  (are [x y] (= x (evaluate (str (quote y))))
    false (> 1 2)
    true  (> 2 1 -1)
    true  (> 2 (1) (-1))
    #?@(:cljs [false (> 1 2.0)]))
  (are [x] (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                    (evaluate (str (quote x))))
    (> 2 (1 2))
    (> 2 "1")
    #?(:clj (> 1 2.0))))

(deftest <-test
  (are [x y] (= x (evaluate (str (quote y))))
    true  (< 1 2)
    true  (< 1 2 (3))
    false (< 2 1 -1)
    false (< 2 (1) (-1))
    #?@(:cljs [true (< 1 2.1)]))
  (are [x] (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                    (evaluate (str (quote x))))
    (< 2 (1 2))
    (< 2 "1")
    #?(:clj (< 1 2.1))))

(deftest >=-test
  (are [x y] (= x (evaluate (str (quote y))))
    false (>= 1 2)
    true  (>= 2 1 -1)
    true  (>= 2 2 -1)
    true  (>= 2 2 2)
    true  (>= 2 (1) (-1))
    #?@(:cljs [false (>= 1 2.1)]))
  (are [x] (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                    (evaluate (str (quote x))))
    (>= 2 (1 2))
    (>= 2 "1")
    #?(:clj (>= 1 2.1))))

(deftest <=-test
  (are [x y] (= x (evaluate (str (quote y))))
    true  (<= 1 2)
    true  (<= 1 2 (3))
    true  (<= 2 2 (3))
    true  (<= 2 2 (2))
    false (<= 2 1 -1)
    false (<= 2 (1) (-1))
    #?@(:cljs [true (<= 1 2.1)]))
  (are [x] (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                    (evaluate (str (quote x))))
    (<= 2 (1 2))
    (<= 2 "1")
    #?(:clj (<= 1 2.1))))

(deftest +-test
  (are [x y] (= x (evaluate (str (quote y))))
    3                           (+ 1 2)
    6                           (+ 1 2 (3))
    #inst "2010-10-10T08:10:00" (+ (date 2010 10 10) (hours 8) (minutes 10))
    "xyz"                       (+ ("x") "y" ("z"))
    #?@(:cljs [3.1 (+ 1 2.1)]))
  (is (thrown? #?(:clj Exception :cljs js/Error)
               (evaluate (str '(+ (date 2010 10 10) (date 1 1 1))))))
  (are [x] (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                    (evaluate (str (quote x))))
    (+ (hours 8) (date 2010 10 10))
    (+ {:x 1} {:y 2})
    (+ 2 "1")
    #?(:clj (+ 1 2.1))
    (+ 2 nil)
    (+ "x" nil)))

(deftest *-test
  (are [x y] (= x (evaluate (str (quote y))))
    2 (* 1 2)
    6 (* 1 2 (3))
    #?@(:cljs [2.1 (* 1 2.1)]))
  (are [x] (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                    (evaluate (str (quote x))))
    (* ("x") "y" ("z"))
    (* {:x 1} {:y 2})
    (* 2 "1")
    #?(:clj (* 1 2.1))
    (* 2 nil))) 

(deftest subtraction-test ; -
  (are [x y] (= x (evaluate (str (quote y))))
    -1                          (- 1 2)
    -4                          (- 1 2 (3))
    #inst "2010-10-10T00:00:00" (- (datetime 2010 10 10 8 10) (hours 8) (minutes 10))
    #?@(:cljs [-1.1 (- 1 2.1)]))
  (is (thrown? #?(:clj Exception :cljs js/Error)
               (evaluate (str '(- (date 2010 10 10) (date 1 1 1))))))
  (are [x] (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                    (evaluate (str (quote x))))
    (- (hours 8) (date 2010 10 10))
    (- {:x 1} {:y 2})
    (- 2 "1")
    #?(:clj (- 1 2.1))
    (- 2 nil)))

(deftest division-test ; /
  (are [x y] (= x (evaluate (str (quote y))))
    2 (/ 4 2)
    3 (/ 30 2 5)
    #?@(:cljs [2 (/ 4 2.0)]))
  (are [x] (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                    (evaluate (str (quote x))))
    (/ {:x 1} {:y 2})
    (/ 2 "1")
    #?(:clj (/ 4 2.0))
    (/ 2 (nil))))

(deftest sum-test
  (are [x y] (= x (evaluate (str (quote y))))
    3 (sum 1 2)
    6 (sum 1 2 (3))
    6 (sum (1 2 3))
    7 (sum (1 2 3) 1)
    #?@(:cljs [3 (sum 1 2.0)]))
  (are [x] (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                    (evaluate (str (quote x))))
    (sum {:x 1} {:y 2})
    (sum 2 "1")
    #?(:clj (sum 1 2.0))
    (sum 2 (2 nil))))

(deftest product-test
  (are [x y] (= x (evaluate (str (quote y))))
    2  (product 1 2)
    6  (product 1 2 (3))
    6  (product (1 2 3))
    24 (product (1 2 3) 4)
    #?@(:cljs [2 (product 1 2.0)]))
  (are [x] (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                    (evaluate (str (quote x))))
    (product ("x") "y" ("z"))
    (product {:x 1} {:y 2})
    (product 2 "1")
    #?(:clj (product 1 2.0))
    (product 2 (2 nil))))

(deftest filter-test
  (are [x y] (= (quote x) (evaluate (str (quote y))))
    (1 1)           (filter (= % 1) (1 2 3) (4 5 6) 1)
    (1 2 3 4 5 6 7) (filter % (1 2 3) (4 5 6 nil false) 7 ())
    ({:x 1} {:x 2}) (filter (:x %) ({:x 1} {:x 2}))
    (nil 1 2 3 4 5) (filter true nil () 1 (2) 3 (4 5))
    (0 1)           (filter (filter %2 %1) (0 1)))
  (is (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
               (evaluate (str '(filter % 0 (1 (2 3) 4) 5))))))

(deftest map-test
  (are [x y] (= (quote x) (evaluate (str (quote y))))
    (2 3 4 5 6)    (map (+ % 1) (1 2 3) 4 (5))
    (1)            (map (:x %) {:x 1})
    (1 nil)        (map (:x %) ({:x 1} {:y 1}))
    (0 nil 1 2 3)  (map % 0 () nil (1) (2 3))
    (10 10 20 20)  (map (map (* 10 %2) % %1) (1 2)))
  (is (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
               (evaluate (str '(map (:x %) (({:x 1} {:y 1}))))))))

(deftest select-keys-test
  (are [x y] (= (quote x) (evaluate (str (quote y))))
    ({:x 1} {:y 1} {:x 3 :y 4}) (select-keys (list :x :y)
                                             ({:x 1 :z 2}
                                              {:y 1 :w 2})
                                             {:x 3 :y 4})
    ({:y 2} {} {:z 4})          (select-keys (if (= (:x %) 1)
                                               (list :y)
                                               (list :z))
                                             ({:x 1 :y 2}
                                              {:x 2 :y 3}
                                              {:x 3 :z 4}))
    ({"x" 1} {"x" 3 111 :a})    (select-keys ("x" 111)
                                             ({"x" 1 "y" 2}
                                              {"x" 3 111 :a}))))

(deftest now-test
  #_(is (= #inst "2021-01-01T20:00" (evaluate (str '(now)))))
  (is (instance? #?(:clj java.util.Date :cljs js/Date) (evaluate (str '(now))))))

(deftest years-test
  (is (instance? java.time.Period (evaluate (str '(years 1))))))

(deftest months-test
  (is (instance? java.time.Period (evaluate (str '(months 7))))))

(deftest weeks-test
  (is (instance? java.time.Period (evaluate (str '(weeks 10))))))

(deftest days-test
  (is (instance? java.time.Period (evaluate (str '(days 30))))))

(deftest hours-test
  (is (instance? java.time.Duration (evaluate (str '(hours 42))))))

(deftest minutes-test
  (is (instance? java.time.Duration (evaluate (str '(minutes 34))))))

(deftest year-start-test
  (is (= #inst "2010-01-01T00:00" (evaluate (str '(year-start (date 2010 10 10)))))))

(deftest month-start-test
  (is (= #inst "2010-10-01T00:00" (evaluate (str '(month-start (date 2010 10 10)))))))

(deftest day-start-test
  (is (= #inst "2010-10-10T00:00" (evaluate (str '(day-start (datetime 2010 10 10 10 00)))))))

(deftest date-test
  (is (= #inst "2010-10-10" (evaluate (str '(date 2010 10 10))))))

(deftest datetime-test
  (is (= #inst "2010-10-10T17:00:00" (evaluate (str '(datetime 2010 10 10 17 00))))))

(deftest at-zone-test
  (is (= #inst "2010-10-10T07:00" (evaluate (str '(at-zone (datetime 2010 10 10 3 0)
                                                           "Europe/Moscow")))))
  (is (= #inst "2015-10-10T06:00" (evaluate (str '(at-zone (datetime 2015 10 10 3 0)
                                                           "Europe/Moscow"))))))

(deftest user-fn-test
  (are [x y z] (= (quote x) (evaluate (str (quote y)) z))
    -4            (user/fn1 1 2 3 4) {'user/fn1 (fn [a b c d] (- (+ a b) (+ c d)))}
    (2 4 6)       (filter (user/fn2 %) (1 2 3) (4 5) 6) {'user/fn2 (fn [el] (even? el))}
    "some string" (user/fn3) {'user/fn3 (fn [] "some string")})
  #?(:clj (is (thrown? Exception
                       (evaluate (str '(user/fn4)) {'user/fn4 (fn [a b] (* a b))}))))
  #?(:cljs (is (= true
                  (.isNaN js/Number (evaluate (str '(user/fn4))
                                              {'user/fn4 (fn [a b] (* a b))})))))
  (is (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
               (evaluate (str '(user/fn5))))))

(deftest evaluate-test
  (is (thrown? #?(:clj clojure.lang.ExceptionInfo
                  :cljs js/Error) (evaluate (str '(a b)))))

  (let [car {:car/name "Bus"
             :car/state [{:id 1
                          :car-state/feature {:db/id 70
                                              :feature/code :color
                                              :feature/name "Color"}
                          :car-state/value-code "black"
                          :car-state/long-value 10.7}
                         {:id 2
                          :car-state/feature {:db/id 80
                                              :feature/code :length
                                              :feature/name "Length"}
                          :car-state/long-value 10.7}
                         {:id 3
                          :car-state/feature {:db/id 90
                                              :feature/code :feature-00001
                                              :feature/name "Feature 00001"}
                          :car-state/long-value 10.7}]}]
    (testing "Complex example #1."
      (is (= (apply list (:car/state car))
             (evaluate (str '(:car/state $)) {'$ car})))
      (is (= '({:id 1
                :car-state/feature {:db/id 70
                                    :feature/code :color
                                    :feature/name "Color"}
                :car-state/value-code "black"
                :car-state/long-value 10.7})
             (evaluate (str '(filter (= (:feature/code (:car-state/feature %)) :color)
                                     (:car/state $)))
                       {'$ car})))
      (is (= true
             (evaluate (str '(= (:car-state/value-code
                                 (filter (= (:feature/code (:car-state/feature %)) :color)
                                         (:car/state $)))
                                "black"))
                       {'$ car}))))
    (testing "Complex example #2."
      (is (= '({:id 1
                :car-state/feature {:db/id 70
                                    :feature/code :color
                                    :feature/name "Color"}
                :car-state/value-code "black"
                :car-state/long-value 10.7}
               {:id 3
                :car-state/feature {:db/id 90
                                    :feature/code :feature-00001
                                    :feature/name "Feature 00001"}
                :car-state/long-value 10.7})
             (evaluate (str '(filter
                              (or (= (:db/id (:car-state/feature %)) 70)
                                  (= (:db/id (:car-state/feature %)) 90))
                              (:car/state $)))
                       {'$ car})))
      (is (= false
             (evaluate (str '(>
                              (sum
                               (:car-state/long-value
                                (filter
                                 (or (= (:db/id (:car-state/feature %)) 70)
                                     (= (:db/id (:car-state/feature %)) 90))
                                 (:car/state $))))
                              1000.0))
                       {'$ car}))))
    (testing "Complex example #3."
      (is (= '({:car/name "Bus"})
             (evaluate (str '(if (filter
                                  (= (:feature/code
                                      (:car-state/feature %))
                                     :feature-00001)
                                  (:car/state $))
                               (select-keys (list :car/name) $)
                               ()))
                       {'$ car}))))
    (testing "Complex example #4."
      (is (= '({})
             (evaluate (str '(select-keys
                              (list :car/name)
                              (filter
                               (= (:feature/code
                                   (:car-state/feature %))
                                  :feature-00001)
                               (:car/state $))))
                       {'$ car}))))
    (testing "Complex example #5."
      (is (= false
             (evaluate (str '(>
                              (+
                               (count
                                (ext/list-cars (now) (- (now) (months 1))))
                               1)
                              10))
                       {'ext/list-cars
                        (fn [a b] '(:car1 :car2 :car3))}))))))

#?(:cljs (run-tests))
