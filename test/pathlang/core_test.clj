(ns pathlang.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [pathlang.core :as core]))

(deftest keyword-test
  (is (= '(1 2 nil)   (core/evaluate '(:foo {:foo 1} ({:foo 2} {:bar 1}) ()))))
  (is (= '(1 2 4 nil) (core/evaluate '(:foo {:foo 1} ({:foo 2} {:foo 4}) ())))))
; ???: (:foo {:foo 1} ({:foo 2} {:bar 1}) ()) => (1 2 nil nil)

(deftest implicit-list-test
  (is (= '(1 2)          (core/evaluate '(1 2))))
  (is (= '(1 2 (3))      (core/evaluate '(1 2 (3)))))
  (is (= '(1 "2" {:x 1}) (core/evaluate '(1 "2" {:x 1}))))
  (is (= '()             (core/evaluate '()))))

(deftest value-test
  (is (= '5              (core/evaluate '5)))
  (is (= 'nil            (core/evaluate 'nil)))
  (is (= '\newline       (core/evaluate '\newline)))
  (is (= '"some str"     (core/evaluate '"some str")))
  (is (= '()             (core/evaluate '())))
  (is (= ':keyword       (core/evaluate ':keyword)))
  (is (= 'q-symbol       (core/evaluate ''q-symbol)))
  (is (thrown? Exception (core/evaluate 'unq-symbol)))
  (is (= 42              (core/evaluate '$ {'$ 42}))))

(deftest hash-map-test
  (is (= {"xy" 3} (core/evaluate '{(+ "x" "y") (+ 1 $)} {'$ 2}))))

(deftest list-test
  (is (= '(1 2)          (core/evaluate '(list 1 2))))
  (is (= '(1 2 (3))      (core/evaluate '(list 1 2 (3)))))
  (is (= '(1 "2" {:x 1}) (core/evaluate '(list 1 "2" {:x 1}))))
  (is (= '()             (core/evaluate '(list)))))

(deftest if-test
  (is (= 1 (core/evaluate '(if (= 1 2)
                             (+ 1 2)
                             (- 2 1)))))
  (is (= 3 (core/evaluate '(if (= 1 1)
                             (+ 1 2)
                             (- 2 1))))))
; ???: more args or less args.

(deftest count-test
  (is (= 3 (core/evaluate '(count (1 2 3)))))
  (is (= 3 (core/evaluate '(count 1 2 3))))
  (is (= 4 (core/evaluate '(count (1) (2 3) 4))))
  (is (= 5 (core/evaluate '(count () nil (1 2 3 ()))))))
; ???: (count nil)
; ???: (count (nil nil 3))
; ???: (count (1 2 3 ()) 4 ())

(deftest =-test
  (is (=       false     (core/evaluate '(= "red" ("green") "blue"))))
  (is (=       true      (core/evaluate '(= "red" ("red") "red"))))
  (is (=       false     (core/evaluate '(= "red" nil))))
  (is (thrown? Exception (core/evaluate '(= "red" ("red" "green")))))
  (is (thrown? Exception (core/evaluate '(= "red" (1) {:x 1}))))
  (is (thrown? Exception (core/evaluate '(= "red" ((1))))))
  (is (thrown? Exception (core/evaluate '(= "red"))))
  (is (thrown? Exception (core/evaluate '(= "red" ())))))

(deftest not=-test
  (is (=       true      (core/evaluate '(not= "red" ("green") "blue"))))
  (is (=       false     (core/evaluate '(not= "red" ("red") "red"))))
  (is (=       true      (core/evaluate '(not= "red" nil))))
  (is (thrown? Exception (core/evaluate '(not= "red" ("red" "green")))))
  (is (thrown? Exception (core/evaluate '(not= "red" (1) {:x 1}))))
  (is (thrown? Exception (core/evaluate '(not= "red" ((1))))))
  (is (thrown? Exception (core/evaluate '(not= "red"))))
  (is (thrown? Exception (core/evaluate '(not= "red" ())))))

(deftest >-test
  (is (=       false     (core/evaluate '(> 1 2))))
  (is (=       true      (core/evaluate '(> 2 1 -1))))
  (is (=       true      (core/evaluate '(> 2 (1) (-1)))))
  (is (thrown? Exception (core/evaluate '(> 2 (1 2)))))
  (is (thrown? Exception (core/evaluate '(> 2 "1")))))

(deftest <-test
  (is (=       true      (core/evaluate '(< 1 2))))
  (is (=       true      (core/evaluate '(< 1 2 (3)))))
  (is (=       false     (core/evaluate '(< 2 1 -1))))
  (is (=       false     (core/evaluate '(< 2 (1) (-1)))))
  (is (thrown? Exception (core/evaluate '(< 2 (1 2)))))
  (is (thrown? Exception (core/evaluate '(< 2 "1")))))

(deftest >=-test
  (is (=       false     (core/evaluate '(>= 1 2))))
  (is (=       true      (core/evaluate '(>= 2 1 -1))))
  (is (=       true      (core/evaluate '(>= 2 2 -1))))
  (is (=       true      (core/evaluate '(>= 2 2 2))))
  (is (=       true      (core/evaluate '(>= 2 (1) (-1)))))
  (is (thrown? Exception (core/evaluate '(>= 2 (1 2)))))
  (is (thrown? Exception (core/evaluate '(>= 2 "1")))))

(deftest <=-test
  (is (=       true      (core/evaluate '(<= 1 2))))
  (is (=       true      (core/evaluate '(<= 1 2 (3)))))
  (is (=       true      (core/evaluate '(<= 2 2 (3)))))
  (is (=       true      (core/evaluate '(<= 2 2 (2)))))
  (is (=       false     (core/evaluate '(<= 2 1 -1))))
  (is (=       false     (core/evaluate '(<= 2 (1) (-1)))))
  (is (thrown? Exception (core/evaluate '(<= 2 (1 2)))))
  (is (thrown? Exception (core/evaluate '(<= 2 "1")))))

(deftest +-test
  (is (=       3         (core/evaluate '(+ 1 2))))
  (is (=       6         (core/evaluate '(+ 1 2 (3)))))
  (is (=       #inst "2010-10-10T08:10:00"
                         (core/evaluate '(+ (date 2010 10 10) (hours 8) (minutes 10)))))
  (is (=       "xyz"     (core/evaluate '(+ ("x") "y" ("z")))))
  (is (thrown? Exception (core/evaluate '(+ {:x 1} {:y 2}))))
  (is (thrown? Exception (core/evaluate '(+ 2 "1")))))

(deftest *-test
  (is (=       2         (core/evaluate '(* 1 2))))
  (is (=       6         (core/evaluate '(* 1 2 (3)))))
  (is (thrown? Exception (core/evaluate '(* ("x") "y" ("z")))))
  (is (thrown? Exception (core/evaluate '(* {:x 1} {:y 2}))))
  (is (thrown? Exception (core/evaluate '(* 2 "1")))))

(deftest subtraction-test ; -
  (is (=       -1        (core/evaluate '(- 1 2))))
  (is (=       -4        (core/evaluate '(- 1 2 (3)))))
  (is (=       #inst "2010-10-10T00:00:00"
                         (core/evaluate '(- (datetime 2010 10 10 8 10) (hours 8) (minutes 10)))))
  (is (thrown? Exception (core/evaluate '(- {:x 1} {:y 2}))))
  (is (thrown? Exception (core/evaluate '(- 2 "1")))))

(deftest division-test
  (is (=       2         (core/evaluate '(/ 4 2))))
  (is (=       3         (core/evaluate '(/ 30 2 5))))
  (is (thrown? Exception (core/evaluate '(/ {:x 1} {:y 2}))))
  (is (thrown? Exception (core/evaluate '(/ 2 "1"))))) ; /

(deftest sum-test
  (is (=       3         (core/evaluate '(sum 1 2))))
  (is (=       6         (core/evaluate '(sum 1 2 (3)))))
  (is (=       6         (core/evaluate '(sum (1 2 3)))))
  (is (=       7         (core/evaluate '(sum (1 2 3) 1))))
  (is (thrown? Exception (core/evaluate '(sum {:x 1} {:y 2}))))
  (is (thrown? Exception (core/evaluate '(sum 2 "1")))))

(deftest product-test
  (is (=       2         (core/evaluate '(product 1 2))))
  (is (=       6         (core/evaluate '(product 1 2 (3)))))
  (is (=       6         (core/evaluate '(product (1 2 3)))))
  (is (=       24        (core/evaluate '(product (1 2 3) 4))))
  (is (thrown? Exception (core/evaluate '(product ("x") "y" ("z")))))
  (is (thrown? Exception (core/evaluate '(product {:x 1} {:y 2}))))
  (is (thrown? Exception (core/evaluate '(product 2 "1")))))

(deftest filter-test
  (is (= '(1 1)           (core/evaluate '(filter (= % 1) (1 2 3) (4 5 6) 1))))
  (is (= '(1 2 3 4 5 6 7) (core/evaluate '(filter % (1 2 3) (4 5 6 nil false) 7 ()))))
  (is (= '(1 2)           (core/evaluate '(filter (:x %) ({:x 1} {:x 2}))))))

(deftest map-test
  (is (= '(2 3 4 5 6) (core/evaluate '(map (+ % 1) (1 2 3) 4 (5)))))
  (is (= '(1)         (core/evaluate '(map (:x %) {:x 1})))))

(deftest select-keys-test
  (is (= '({:x 1} {:y 1} {:x 3 :y 4})
         (core/evaluate '(select-keys (list :x :y)
                                      ({:x 1 :z 2} {:y 1 :w 2})
                                      {:x 3 :y 4}))))
  (is (= '({:y 2} {} {:z 4})
         (core/evaluate '(select-keys (if (= (:x %) 1)
                                        (list :y)
                                        (list :z))
                                      ({:x 1 :y 2}
                                       {:x 2 :y 3}
                                       {:x 3 :z 4}))))))

(deftest now-test
  (is (= #inst "2021-01-01T20:00" (core/evaluate '(now)))))

(deftest years-test
  ;(is (= <not graphical representation> (core/evaluate '(years 1))))
  )

(deftest months-test
  ;(is (= <not graphical representation> (core/evaluate '(months 7))))
  )

(deftest days-test)

(deftest hours-test)

(deftest minutes-test)

(deftest year-start-test
  (is (= #inst "2010-01-01T00:00" (core/evaluate '(year-start (date 2010 10 10))))))

(deftest month-start-test
  (is (= #inst "2010-10-01T00:00" (core/evaluate '(month-start (date 2010 10 10))))))

(deftest day-start-test
  (is (= #inst "2010-10-10T00:00" (core/evaluate '(day-start (datetime 2010 10 10 10 00))))))

(deftest date-test
  (is (= #inst "2010-10-10" (core/evaluate '(date 2010 10 10)))))

(deftest datetime-test
  (is (= #inst "2010-10-10T17:00:00" (core/evaluate '(datetime 2010 10 10 17 00)))))

(deftest at-zone-test
  ;(is (= #inst "2010-10-10T06:00" (core/evaluate '(at-zone (datetime 2010 10 10 3 0) <Moscow timezone>))))
  )