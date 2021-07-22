(ns pathlang.stdlib
  (:require [pathlang.stdlib.time :as time]
            [clojure.spec.alpha :as s]
            [pathlang.spec :as pls]
            [pathlang.helpers :as help :refer [atomic-value? date?]]
            [pathlang.stdlib.utils :refer [make-constraint check-types
                                           logical-false? logical-true?]]))

(defn pl-list [& args]
  (help/flatten-top-level args))

(defn pl-count [arg1 & args]
  (->> (cons arg1 args)
       (map #(if (-> % atomic-value? not) (count %) 1))
       (apply +)))

(defn pl-keyword [kw arg1 & args]
  (->> (cons arg1 args)
       (help/flatten-top-level)
       (map #(get % kw))
       (help/flatten-top-level)))

(defn pl-= [arg1 arg2 & args]
  (->> (cons arg1 (cons arg2 args))
       (help/flatten-top-level)
       (#(check-types pl-= % :ignore-nil true))
       (apply =)))

(defn pl-not= [arg1 arg2 & args]
  (->> (cons arg1 (cons arg2 args))
       (help/flatten-top-level)
       (#(check-types pl-not= % :ignore-nil true))
       (apply not=)))

(defn pl-or [arg1 arg2 & args]
  (->> (cons arg1 (cons arg2 args))
       (some #(logical-true? (%)))
       (#(or % false))))

(defn pl-and [arg1 arg2 & args]
  (->> (cons arg1 (cons arg2 args))
       (some #(logical-false? (%)))
       (not)))

(defn pl-not [arg1]
  (logical-false? arg1))

(defn pl-> [arg1 arg2 & args]
  (->> (cons arg1 (cons arg2 args))
       (help/flatten-top-level)
       (check-types pl->)
       (apply >)))

(defn pl-< [arg1 arg2 & args]
  (->> (cons arg1 (cons arg2 args))
       (help/flatten-top-level)
       (check-types pl-<)
       (apply <)))

(defn pl->= [arg1 arg2 & args]
  (->> (cons arg1 (cons arg2 args))
       (help/flatten-top-level)
       (check-types pl->=)
       (apply >=)))

(defn pl-<= [arg1 arg2 & args]
  (->> (cons arg1 (cons arg2 args))
       (help/flatten-top-level)
       (check-types pl-<=)
       (apply <=)))

(defn pl-+ [arg1 arg2 & args]
  (let [args (->> (cons arg1 (cons arg2 args))
                  (help/flatten-top-level)
                  (#(check-types pl-+ % :type-checkers [number? string? date?])))
        arg1 (first args)]
    (cond
      (number? arg1) (apply + args)
      (string? arg1) (apply str args)
      (date? arg1) (apply time/add args)
      :else (throw (ex-info (str "Type checking ended with exception due to the absence of "
                                 "the corresponding branch in the cond function.")
                            {:cause :pathlang-internal-type-checking-exception
                             :arg-value arg1
                             :arg-name 'arg1
                             :type-checkers [number? date?]})))))

(defn pl-* [arg1 arg2 & args]
  (let [args (->> (cons arg1 (cons arg2 args))
                  (help/flatten-top-level)
                  (#(check-types pl-* % :type-checkers [number?])))]
    (apply * args)))

; -
(defn pl-subtraction [arg1 arg2 & args]
  (let [args (->> (cons arg1 (cons arg2 args))
                  (help/flatten-top-level)
                  (#(check-types pl-+ % :type-checkers [number? date?])))
        arg1 (first args)]
    (cond
      (number? arg1) (apply - args)
      (date? arg1) (apply time/subtract args)
      :else (throw (ex-info (str "Type checking ended with exception due to the absence of "
                                 "the corresponding branch in the cond function.")
                            {:cause :pathlang-internal-type-checking-exception
                             :arg-value arg1
                             :arg-name 'arg1
                             :type-checkers [number? date?]})))))

; /
(defn pl-division [arg1 arg2 & args]
  (let [args (->> (cons arg1 (cons arg2 args))
                  (help/flatten-top-level)
                  (#(check-types pl-division % :type-checkers [number?])))]
    (apply / args)))

(defn pl-sum [arg1 & args]
  (let [args (->> (cons arg1 args)
                  (help/flatten-top-level)
                  (#(check-types pl-sum % :type-checkers [number?])))]
    (apply + args)))

(defn pl-product [arg1 & args]
  (let [args (->> (cons arg1 args)
                  (help/flatten-top-level)
                  (#(check-types pl-product % :type-checkers [number?])))]
    (apply * args)))

(defn pl-filter [pred arg1 & args]
  (->> (cons arg1 args)
       (help/flatten-top-level)
       (filter #(logical-true? (pred %)))
       (help/flatten-top-level)))

(defn pl-map [f arg1 & args]
  (->> (cons arg1 args)
       (help/flatten-top-level)
       (map f)
       (help/flatten-top-level)))

(defn pl-if [test then else]
  (if (logical-true? test)
    (then)
    (else)))
 
(defn pl-select-keys [f arg1 & args]
  (->> (cons arg1 args)
       (help/flatten-top-level)
       (map #(select-keys % (f %)))))

(def fns
  {'list        (-> (make-constraint [] ::pls/atomic-val-1)
                    (partial pl-list))
   'count       (-> (make-constraint [::pls/atomic-val-0+] ::pls/atomic-val-0+)
                    (partial pl-count))
   'keyword     (-> (make-constraint [keyword? ::pls/map-val-0+] ::pls/map-val-0+)
                    (partial pl-keyword))
   '=           (-> (make-constraint ::pls/atomic-val-1 2 true)
                    (partial pl-=))
   'not=        (-> (make-constraint ::pls/atomic-val-1 2 true)
                    (partial pl-not=))
   'or          (-> (make-constraint [fn? fn?] fn?)
                    (partial pl-or))
   'and         (-> (make-constraint [fn? fn?] fn?)
                    (partial pl-and))
   'not         (-> (make-constraint [::pls/atomic-val-0-1])
                    (partial pl-not))
   '>           (-> (make-constraint ::pls/atomic-val-1 2 true)
                    (partial pl->))
   '<           (-> (make-constraint ::pls/atomic-val-1 2 true)
                    (partial pl-<))
   '>=          (-> (make-constraint ::pls/atomic-val-1 2 true)
                    (partial pl->=))
   '<=          (-> (make-constraint ::pls/atomic-val-1 2 true)
                    (partial pl-<=))
   '+           (-> (make-constraint ::pls/atomic-val-1 2 true)
                    (partial pl-+))
   '*           (-> (make-constraint ::pls/atomic-val-1 2 true)
                    (partial pl-*))
   '-           (-> (make-constraint ::pls/atomic-val-1 2 true)
                    (partial pl-subtraction))
   '/           (-> (make-constraint ::pls/atomic-val-1 2 true)
                    (partial pl-division))
   'sum         (-> (make-constraint [::pls/atomic-val-0+] ::pls/atomic-val-0+)
                    (partial pl-sum))
   'product     (-> (make-constraint [::pls/atomic-val-0+] ::pls/atomic-val-0+)
                    (partial pl-product))
   'filter      (-> (make-constraint [fn? ::pls/atomic-val-0+] ::pls/atomic-val-0+)
                    (partial pl-filter))
   'map         (-> (make-constraint [fn? ::pls/atomic-val-0+] ::pls/atomic-val-0+)
                    (partial pl-map))
   'if          (-> (make-constraint [any? fn? fn?])
                    (partial pl-if))
   'select-keys (-> (make-constraint [fn? ::pls/map-val-0+] ::pls/map-val-0+)
                    (partial pl-select-keys))
   'now         (-> (make-constraint [])
                    (partial time/now))
   'years       (-> (make-constraint [int?])
                    (partial time/years))
   'months      (-> (make-constraint [int?])
                    (partial time/months))
   'weeks       (-> (make-constraint [int?])
                    (partial time/weeks))
   'days        (-> (make-constraint [int?])
                    (partial time/days))
   'hours       (-> (make-constraint [int?])
                    (partial time/hours))
   'minutes     (-> (make-constraint [int?])
                    (partial time/minutes))
   'year-start  (-> (make-constraint [date?])
                    (partial time/year-start))
   'month-start (-> (make-constraint [date?])
                    (partial time/month-start))
   'day-start   (-> (make-constraint [date?])
                    (partial time/day-start))
   'date        (-> (make-constraint [int? pos-int? pos-int?])
                    (partial time/date))
   'datetime    (-> (make-constraint [int? pos-int? pos-int? nat-int? nat-int?])
                    (partial time/datetime))
   'at-zone     (-> (make-constraint [date? string?])
                    (partial time/at-zone))})
