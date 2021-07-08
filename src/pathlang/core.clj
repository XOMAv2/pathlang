(ns pathlang.core
  (:require [clojure.spec.alpha :as s]
            [pathlang.helpers :as help :refer [atomic-value?]]
            [pathlang.time :as time]
            [pathlang.spec :as pls]))

(def std-fns #{'list 'if 'count ; :keyword :implicit-list :value
               '= 'not= '> '< '>= '<=
               '+ '* '- '/ 'sum 'product
               'filter 'map 'select-keys
               'now 'years 'months 'weeks 'days 'hours 'minutes
               'year-start 'month-start 'day-start
               'date 'datetime 'at-zone})

(defn get-fn
  [expression context]
  (cond (map? expression)
        :hash-map
        
        (coll? expression)
        (let [first-el (first expression)]
          (cond (contains? std-fns first-el) first-el
                (keyword? first-el) :keyword
                (contains? (dissoc context '$) first-el) :user-fn
                :else :implicit-list))

        :else :value))

(defmulti ^:private pl-eval
  #'get-fn)

(defmethod pl-eval :value
  [expression context]
  (cond (and (symbol? expression) (contains? context expression))
        (get context expression)
        
        (symbol? expression)
        (throw (Exception. (str "Pathlang runtime exception. "
                                "Unable to resolve symbol: " expression " in this context.")))
        
        :else expression))

(defmethod pl-eval :hash-map
  [expression context]
  (->> expression
       (map (fn [[k v]]
              [(pl-eval k context) (pl-eval v context)]))
       (into {})))

(defmethod pl-eval :keyword
  [[keyword & args] context]
  (let [args (map #(pl-eval % context) args)
        args (help/flatten-top-level args)]
    (map #(get % keyword) args)))

(defmethod pl-eval 'list
  [[_ & args] context]
  (map #(pl-eval % context) args))

(defmethod pl-eval :implicit-list
  [expression context]
  (map #(pl-eval % context) expression))

(defmethod pl-eval 'if
  [[_ test t-branch f-branch :as expression] context]
  (when (not= 4 (count expression))
    (throw (Exception. (str "Pathlang syntax exception."
                            "The if function expects exactly 3 arguments."))))
  (if (pl-eval test context)
    (pl-eval t-branch context)
    (pl-eval f-branch context)))

(defmethod pl-eval 'count
  [[_ & args] context]
  (->> (map #(pl-eval % context) args)
       (map #(if (-> % atomic-value? not) (count %) 1))
       (apply +)))

(defn check-and-flatten-args [args & {:keys [ignore-nil check-types]
                                      :or {ignore-nil false
                                           check-types true}}]
  (let [_ (when (not (help/each-arg-a-val-or-a-single-val-coll? args))
            (throw (Exception. (str "Pathlang syntax exception. "
                                    "Each function argument must be a value or "
                                    "a collection of a single value."))))
        args (help/flatten-top-level args)
        _ (when (< (count args) 2)
            (throw (Exception. (str "Pathlang syntax exception. "
                                    "Function accepts two or more arguments."))))
        _ (when (and check-types
                     (not (help/same-top-level-type? args :ignore-nil ignore-nil)))
            (throw (Exception. (str "Pathlang syntax exception. "
                                    "Each atomic value must have the same type."))))]
    args))

(defmethod pl-eval '=
  [[_ & args] context]
  (let [args (map #(pl-eval % context) args)
        args (check-and-flatten-args args :ignore-nil true)]
    (apply = args)))

(defmethod pl-eval 'not=
  [[_ & args] context]
  (let [args (map #(pl-eval % context) args)
        args (check-and-flatten-args args :ignore-nil true)]
    (apply not= args)))

(defmethod pl-eval '>
  [[_ & args] context]
  (let [args (map #(pl-eval % context) args)
        args (check-and-flatten-args args)]
    (apply > args)))

(defmethod pl-eval '<
  [[_ & args] context]
  (let [args (map #(pl-eval % context) args)
        args (check-and-flatten-args args)]
    (apply < args)))

(defmethod pl-eval '>=
  [[_ & args] context]
  (let [args (map #(pl-eval % context) args)
        args (check-and-flatten-args args)]
    (apply >= args)))

(defmethod pl-eval '<=
  [[_ & args] context]
  (let [args (map #(pl-eval % context) args)
        args (check-and-flatten-args args)]
    (apply <= args)))

(defmethod pl-eval '+
  [[_ & args] context]
  (let [args (map #(pl-eval % context) args)
        args (check-and-flatten-args args :check-types false)
        arg1 (first args)
        _ (when (not (or (help/same-top-level-type? args)
                         (instance? java.util.Date (first args))))
            (throw (Exception. (str "Pathlang syntax exception. "
                                    "Each atomic value must have the same type."))))]
    (cond
      (number? arg1) (apply + args)
      (string? arg1) (apply str args)
      (instance? java.util.Date arg1) (apply time/add args)
      :else (throw (Exception. (str "Pathlang syntax exception. "
                                    "The + function supports only numbers, strings, and dates."))))))

(defmethod pl-eval '*
  [[_ & args] context]
  (let [args (map #(pl-eval % context) args)
        args (check-and-flatten-args args)]
    (if (number? (first args))
      (apply * args)
      (throw (Exception. (str "Pathlang syntax exception. "
                              "The * function supports only numbers."))))))

(defmethod pl-eval '-
  [[_ & args] context]
  (let [args (map #(pl-eval % context) args)
        args (check-and-flatten-args args :check-types false)
        arg1 (first args)
        _ (when (not (or (help/same-top-level-type? args)
                         (instance? java.util.Date (first args))))
            (throw (Exception. (str "Pathlang syntax exception. "
                                    "Each atomic value must have the same type."))))]
    (cond
      (number? arg1) (apply - args)
      (instance? java.util.Date arg1) (apply time/subtract args)
      :else (throw (Exception. (str "Pathlang syntax exception. "
                                    "The - function supports only numbers and dates."))))))

(defmethod pl-eval '/
  [[_ & args] context]
  (let [args (map #(pl-eval % context) args)
        args (check-and-flatten-args args)
        arg1 (first args)]
    (if (number? arg1)
      (apply / args)
      (throw (Exception. (str "Pathlang syntax exception. "
                              "The / function supports only numbers and dates."))))))

(defmethod pl-eval 'sum
  [[_ & args] context]
  (let [args (map #(pl-eval % context) args)
        args (help/flatten-top-level args)
        _ (when (not (help/same-top-level-type? args))
            (throw (Exception. (str "Pathlang syntax exception. "
                                    "Each atomic value must have the same type."))))]
    (if (number? (first args))
      (apply + args)
      (throw (Exception. (str "Pathlang syntax exception. "
                              "The sum function supports only numbers."))))))

(defmethod pl-eval 'product
  [[_ & args] context]
  (let [args (map #(pl-eval % context) args)
        args (help/flatten-top-level args)
        _ (when (not (help/same-top-level-type? args))
            (throw (Exception. (str "Pathlang syntax exception. "
                                    "Each atomic value must have the same type."))))]
    (if (number? (first args))
      (apply * args)
      (throw (Exception. (str "Pathlang syntax exception. "
                              "The product function supports only numbers."))))))

(defmethod pl-eval 'filter
  [[_ pred & args :as expression] context]
  (when (< (count expression) 3)
    (throw (Exception. (str "Pathlang syntax exception. "
                            "The filter function expects two or more arguments."))))
  (let [args (map #(pl-eval % context) args)
        args (help/flatten-top-level args :keep-empty-lists true)]
    (filter (fn [curr]
              (let [context (assoc context '% curr)
                    pred-result (pl-eval pred context)
                    _ (when (and (-> pred-result atomic-value? not) (> (count pred-result) 1))
                        (throw (Exception.
                                (str "Pathlang runtime exception. "
                                     "The filter predicate must return atomic value, "
                                     "empty collection or collection with one element. "
                                     "Returned " pred-result " on element " curr "."))))]
                (not (contains? #{false () nil} pred-result))))
            args)))

(defmethod pl-eval 'map
  [[_ pred & args :as expression] context]
  (when (< (count expression) 3)
    (throw (Exception. (str "Pathlang syntax exception. "
                            "The map function expects two or more arguments."))))
  (let [args (map #(pl-eval % context) args)
        args (help/flatten-top-level args :keep-empty-lists true)
        result (reduce (fn [acc curr]
                         (let [context (assoc context '% curr)
                               pred-result (pl-eval pred context)]
                           (if (and (-> pred-result atomic-value? not) (seq pred-result))
                             (into acc pred-result)
                             (conj acc pred-result))))
                       [] args)]
    (apply list result)))

(defmethod pl-eval 'select-keys
  [[_ pred & args :as expression] context]
  (when (< (count expression) 3)
    (throw (Exception. (str "Pathlang syntax exception. "
                            "The select-keys function expects two or more arguments."))))
  (let [args (map #(pl-eval % context) args)
        args (help/flatten-top-level args :keep-empty-lists true)
        _ (when (or (not (help/same-top-level-type? args))
                    (not= (type {}) (type (first args)))) ; ???: hash-map or datomic entity
            (throw (Exception. "Pathlang syntax exception. "
                               "Each atomic value must be the hash-map.")))]
    (map (fn [curr]
           (->> (assoc context '% curr)
                (pl-eval pred)
                (select-keys curr)))
         args)))

(defmethod pl-eval 'now
  [_ _]
  (time/now))

(defmethod pl-eval 'years
  [[_ n] context]
  (time/years (pl-eval n context)))

(defmethod pl-eval 'months
  [[_ n] context]
  (time/months (pl-eval n context)))

(defmethod pl-eval 'weeks
  [[_ n] context]
  (time/weeks (pl-eval n context)))

(defmethod pl-eval 'days
  [[_ n] context]
  (time/days (pl-eval n context)))

(defmethod pl-eval 'hours
  [[_ n] context]
  (time/hours (pl-eval n context)))

(defmethod pl-eval 'minutes
  [[_ n] context]
  (time/minutes (pl-eval n context)))

(defmethod pl-eval 'year-start
  [[_ d] context]
  (time/year-start (pl-eval d context)))

(defmethod pl-eval 'month-start
  [[_ d] context]
  (time/month-start (pl-eval d context)))

(defmethod pl-eval 'day-start
  [[_ d] context]
  (time/day-start (pl-eval d context)))

(defmethod pl-eval 'date
  [[_ year month day] context]
  (time/date (pl-eval year context)
             (pl-eval month context)
             (pl-eval day context)))

(defmethod pl-eval 'datetime
  [[_ year month day hour minute] context]
  (time/datetime (pl-eval year context)
                 (pl-eval month context)
                 (pl-eval day context)
                 (pl-eval hour context)
                 (pl-eval minute context)))

(defmethod pl-eval 'at-zone
  [[_ datetime timezone] context]
  (time/at-zone (pl-eval datetime context)
                (pl-eval timezone context)))

(defmethod pl-eval :user-fn
  [[fn-name & args] context]
  (let [args (map #(pl-eval % context) args)
        fn-name (pl-eval fn-name context)]
    (apply fn-name args)))

(defmethod pl-eval :default
  [_ _]
  (throw (Exception. "Pathlang syntax exception.")))

(defn evaluate
  "Wrapper over the pl-eval function for argument validation."
  ([expression] (evaluate expression {}))
  ([expression context]
   (let [expression (read-string expression)]
     (when (not (s/valid? ::pls/expression expression))
       (throw (Exception. (str "Pathlang syntax exception in the evaluation expression.\n"
                               (help/beautiful-spec-explain ::pls/expression expression)))))
     (when (not (s/valid? ::pls/context context))
       (throw (Exception. (str "Pathlang syntax exception in the evaluation context.\n"
                               (help/beautiful-spec-explain ::pls/context context)))))
     (let [result (pl-eval expression context)]
       (if (seq? result)
         (doall result)
         result)))))

#_(evaluate "(ext/kek $)" {'$ 42 'ext/kek (fn [a] a)})
