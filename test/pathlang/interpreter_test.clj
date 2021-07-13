(ns pathlang.interpreter-test
  (:require [clojure.test :as test :refer [deftest testing is]]
            [pathlang.interpreter :as pl :refer [evaluate]]))

(deftest keyword-test
  (is (= '(1 2 nil)      (evaluate (str '(:foo {:foo 1} ({:foo 2} {:bar 1}) ())))))
  (is (= '(1 2 4)        (evaluate (str '(:foo {:foo 1} ({:foo 2} {:foo 4}) ())))))
  (is (= '()             (evaluate (str '(:foo ())))))
  (is (= '(1 2 3 4)      (evaluate (str '(:foo {:foo (1 2 3)} {:foo (4)} {:foo ()})))))
  (is (= '(1)            (evaluate (str '(:foo {:foo 1})))))
  (is (thrown? Exception (evaluate (str '(:foo nil)))))
  (is (thrown? Exception (evaluate (str '(:foo ((1))))))))

(deftest implicit-list-test
  (is (= '(1 2)          (evaluate (str '(1 2)))))
  (is (= '(1 2 3)        (evaluate (str '(1 2 (3))))))
  (is (= '(1 "2" {:x 1}) (evaluate (str '(1 "2" {:x 1})))))
  (is (= '()             (evaluate (str '()))))
  (is (thrown? Exception (evaluate (str '(()))))))

(deftest value-test
  (is (thrown? Exception (evaluate (str 5))))
  (is (thrown? Exception (evaluate (str nil))))
  (is (= 5               (#'pl/pl-eval 5 {})))
  (is (= true            (#'pl/pl-eval true {})))
  (is (= nil             (#'pl/pl-eval nil {})))
  (is (= \newline        (#'pl/pl-eval \newline {})))
  (is (= "some str"      (#'pl/pl-eval "some str" {})))
  (is (= ()              (#'pl/pl-eval () {})))
  (is (= :keyword        (#'pl/pl-eval :keyword {})))
  (is (thrown? Exception (doall (#'pl/pl-eval ''q-symbol {}))))
  (is (thrown? Exception (#'pl/pl-eval 'unq-symbol {})))
  (is (= 42              (#'pl/pl-eval '$ {'$ 42}))))

(deftest hash-map-test
  (is (= {"xy" 3} (#'pl/pl-eval '{(+ "x" "y") (+ 1 $)} {'$ 2}))))

(deftest list-test
  (is (= '(1 2)          (evaluate (str '(list 1 2)))))
  (is (= '(1 2 3)        (evaluate (str '(list 1 2 (3))))))
  (is (= '(1 "2" {:x 1}) (evaluate (str '(list 1 "2" {:x 1})))))
  (is (= '()             (evaluate (str '(list)))))
  (is (thrown? Exception (evaluate (str '(list ()))))))

(deftest if-test
  (is (= 1               (evaluate (str '(if (= 1 2)
                                           (+ 1 2)
                                           (- 2 1))))))
  (is (= 3               (evaluate (str '(if (= 1 1)
                                           (+ 1 2)
                                           (- 2 1))))))
  (is (thrown? Exception (evaluate (str '(if (= 5 5)
                                           "success"))))))

(deftest count-test
  (is (= 3 (evaluate (str '(count (1 2 3))))))
  (is (= 3 (evaluate (str '(count 1 2 3)))))
  (is (= 4 (evaluate (str '(count (1) (2 3) 4)))))
  (is (= 5 (evaluate (str '(count () nil (1 2 3 nil))))))
  (is (= 1 (evaluate (str '(count nil)))))
  (is (= 0 (evaluate (str '(count ())))))
  (is (= 3 (evaluate (str '(count (+ 1 2) (:foo {:foo 1} {:bar 2} ())))))))

(deftest =-test
  (is (=       false     (evaluate (str '(= "red" ("green") "blue")))))
  (is (=       true      (evaluate (str '(= "red" ("red") "red")))))
  (is (=       false     (evaluate (str '(= "red" nil)))))
  (is (thrown? Exception (evaluate (str '(= "red" ("red" "green"))))))
  (is (thrown? Exception (evaluate (str '(= "red" (1) {:x 1})))))
  (is (thrown? Exception (evaluate (str '(= "red" ((1)))))))
  (is (thrown? Exception (evaluate (str '(= "red")))))
  (is (thrown? Exception (evaluate (str '(= "red" ()))))))

(deftest not=-test
  (is (=       true      (evaluate (str '(not= "red" ("green") "blue")))))
  (is (=       false     (evaluate (str '(not= "red" ("red") "red")))))
  (is (=       true      (evaluate (str '(not= "red" nil)))))
  (is (thrown? Exception (evaluate (str '(not= "red" ("red" "green"))))))
  (is (thrown? Exception (evaluate (str '(not= "red" (1) {:x 1})))))
  (is (thrown? Exception (evaluate (str '(not= "red" ((1)))))))
  (is (thrown? Exception (evaluate (str '(not= "red" ((1)))))))
  (is (thrown? Exception (evaluate (str '(not= "red")))))
  (is (thrown? Exception (evaluate (str '(not= "red" ()))))))

(deftest >-test
  (is (=       false     (evaluate (str '(> 1 2)))))
  (is (=       true      (evaluate (str '(> 2 1 -1)))))
  (is (=       true      (evaluate (str '(> 2 (1) (-1))))))
  (is (thrown? Exception (evaluate (str '(> 2 (1 2))))))
  (is (thrown? Exception (evaluate (str '(> 2 "1"))))))

(deftest <-test
  (is (=       true      (evaluate (str '(< 1 2)))))
  (is (=       true      (evaluate (str '(< 1 2 (3))))))
  (is (=       false     (evaluate (str '(< 2 1 -1)))))
  (is (=       false     (evaluate (str '(< 2 (1) (-1))))))
  (is (thrown? Exception (evaluate (str '(< 2 (1 2))))))
  (is (thrown? Exception (evaluate (str '(< 2 "1"))))))

(deftest >=-test
  (is (=       false     (evaluate (str '(>= 1 2)))))
  (is (=       true      (evaluate (str '(>= 2 1 -1)))))
  (is (=       true      (evaluate (str '(>= 2 2 -1)))))
  (is (=       true      (evaluate (str '(>= 2 2 2)))))
  (is (=       true      (evaluate (str '(>= 2 (1) (-1))))))
  (is (thrown? Exception (evaluate (str '(>= 2 (1 2))))))
  (is (thrown? Exception (evaluate (str '(>= 2 "1"))))))

(deftest <=-test
  (is (=       true      (evaluate (str '(<= 1 2)))))
  (is (=       true      (evaluate (str '(<= 1 2 (3))))))
  (is (=       true      (evaluate (str '(<= 2 2 (3))))))
  (is (=       true      (evaluate (str '(<= 2 2 (2))))))
  (is (=       false     (evaluate (str '(<= 2 1 -1)))))
  (is (=       false     (evaluate (str '(<= 2 (1) (-1))))))
  (is (thrown? Exception (evaluate (str '(<= 2 (1 2))))))
  (is (thrown? Exception (evaluate (str '(<= 2 "1"))))))

(deftest +-test
  (is (=       3         (evaluate (str '(+ 1 2)))))
  (is (=       6         (evaluate (str '(+ 1 2 (3))))))
  (is (=       #inst "2010-10-10T08:10:00"
                         (evaluate (str '(+ (date 2010 10 10) (hours 8) (minutes 10))))))
  (is (thrown? Exception (evaluate (str '(+ (hours 8) (date 2010 10 10))))))
  (is (thrown? Exception (evaluate (str '(+ (date 2010 10 10) (date 1 1 1))))))
  (is (=       "xyz"     (evaluate (str '(+ ("x") "y" ("z"))))))
  (is (thrown? Exception (evaluate (str '(+ {:x 1} {:y 2})))))
  (is (thrown? Exception (evaluate (str '(+ 2 "1")))))
  (is (thrown? Exception (evaluate (str '(+ 2 nil)))))
  (is (thrown? Exception (evaluate (str '(+ "x" nil))))))

(deftest *-test
  (is (=       2         (evaluate (str '(* 1 2)))))
  (is (=       6         (evaluate (str '(* 1 2 (3))))))
  (is (thrown? Exception (evaluate (str '(* ("x") "y" ("z"))))))
  (is (thrown? Exception (evaluate (str '(* {:x 1} {:y 2})))))
  (is (thrown? Exception (evaluate (str '(* 2 "1")))))
  (is (thrown? Exception (evaluate (str '(* 2 nil)))))) 

(deftest subtraction-test ; -
  (is (=       -1        (evaluate (str '(- 1 2)))))
  (is (=       -4        (evaluate (str '(- 1 2 (3))))))
  (is (=       #inst "2010-10-10T00:00:00"
               (evaluate (str '(- (datetime 2010 10 10 8 10) (hours 8) (minutes 10))))))
  (is (thrown? Exception (evaluate (str '(- (hours 8) (date 2010 10 10))))))
  (is (thrown? Exception (evaluate (str '(- (date 2010 10 10) (date 1 1 1))))))
  (is (thrown? Exception (evaluate (str '(- {:x 1} {:y 2})))))
  (is (thrown? Exception (evaluate (str '(- 2 "1")))))
  (is (thrown? Exception (evaluate (str '(- 2 nil))))))

(deftest division-test ; /
  (is (=       2         (evaluate (str '(/ 4 2)))))
  (is (=       3         (evaluate (str '(/ 30 2 5)))))
  (is (thrown? Exception (evaluate (str '(/ {:x 1} {:y 2})))))
  (is (thrown? Exception (evaluate (str '(/ 2 "1")))))
  (is (thrown? Exception (evaluate (str '(/ 2 (nil)))))))

(deftest sum-test
  (is (=       3         (evaluate (str '(sum 1 2)))))
  (is (=       6         (evaluate (str '(sum 1 2 (3))))))
  (is (=       6         (evaluate (str '(sum (1 2 3))))))
  (is (=       7         (evaluate (str '(sum (1 2 3) 1)))))
  (is (thrown? Exception (evaluate (str '(sum {:x 1} {:y 2})))))
  (is (thrown? Exception (evaluate (str '(sum 2 "1")))))
  (is (thrown? Exception (evaluate (str '(sum 2 (2 nil)))))))

(deftest product-test
  (is (=       2         (evaluate (str '(product 1 2)))))
  (is (=       6         (evaluate (str '(product 1 2 (3))))))
  (is (=       6         (evaluate (str '(product (1 2 3))))))
  (is (=       24        (evaluate (str '(product (1 2 3) 4)))))
  (is (thrown? Exception (evaluate (str '(product ("x") "y" ("z"))))))
  (is (thrown? Exception (evaluate (str '(product {:x 1} {:y 2})))))
  (is (thrown? Exception (evaluate (str '(product 2 "1")))))
  (is (thrown? Exception (evaluate (str '(product 2 (2 nil)))))))

(deftest filter-test
  (is (= '(1 1)           (evaluate (str '(filter (= % 1) (1 2 3) (4 5 6) 1)))))
  (is (= '(1 2 3 4 5 6 7) (evaluate (str '(filter % (1 2 3) (4 5 6 nil false) 7 ())))))
  (is (= '({:x 1} {:x 2}) (evaluate (str '(filter (:x %) ({:x 1} {:x 2}))))))
  (is (= '(nil 1 2 3 4 5) (evaluate (str '(filter true nil () 1 (2) 3 (4 5))))))
  (is (thrown? Exception  (evaluate (str '(filter (filter (= % 2) %) (0 1))))))
  (is (thrown? Exception  (evaluate (str '(filter % 0 (1 (2 3) 4) 5))))))

(deftest map-test
  (is (= '(2 3 4 5 6)    (evaluate (str '(map (+ % 1) (1 2 3) 4 (5))))))
  (is (= '(1)            (evaluate (str '(map (:x %) {:x 1})))))
  (is (= '(1 nil)        (evaluate (str '(map (:x %) (({:x 1} {:y 1})))))))
  (is (= '(0 nil 1 2 3)  (evaluate (str '(map % 0 () nil (1) (2 3))))))
  (is (thrown? Exception (evaluate (str '(map (map (+ 1 %) %) (1 2 3) (4 5 6)))))))

(deftest select-keys-test
  (is (= '({:x 1} {:y 1} {:x 3 :y 4})
         (evaluate (str '(select-keys (list :x :y) ({:x 1 :z 2} {:y 1 :w 2}) {:x 3 :y 4})))))
  (is (= '({:y 2} {} {:z 4})
         (evaluate (str '(select-keys (if (= (:x %) 1)
                                        (list :y)
                                        (list :z))
                                      ({:x 1 :y 2}
                                       {:x 2 :y 3}
                                       {:x 3 :z 4}))))))
  (is (= '({"x" 1} {"x" 3 111 :a})
         (evaluate (str '(select-keys ("x" 111)
                                      ({"x" 1 "y" 2}
                                       {"x" 3 111 :a})))))))

(deftest now-test
  #_(is (= #inst "2021-01-01T20:00" (evaluate (str '(now))))))

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
  (is (= -4              (evaluate (str '(user/fn1 1 2 3 4))
                                        {'user/fn1 (fn [a b c d] (- (+ a b) (+ c d)))})))
  (is (= '(2 4 6)        (evaluate (str '(filter (user/fn2 %) (1 2 3) (4 5) 6))
                                        {'user/fn2 (fn [el] (even? el))})))
  (is (= "some string"   (evaluate (str '(user/fn3))
                                        {'user/fn3 (fn [] "some string")})))
  (is (thrown? Exception (evaluate (str '(user/fn4))
                                        {'user/fn4 (fn [a b] (* a b))})))
  (is (thrown? Exception (evaluate (str '(user/fn5))))))

(deftest evaluate-test
  (is (thrown? Exception (evaluate (str '(a b))))))
