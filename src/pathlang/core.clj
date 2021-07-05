(ns pathlang.core
  (:require [clojure.spec.alpha :as s]
            [pathlang.helpers :as help]
            [pathlang.spec :as pls]))

(def std-fns #{'list 'if 'count ; :keyword :implicit-list :value
               '= 'not= '> '< '>= '<=
               '+ '* '- '/ 'sum 'product
               'filter 'map 'select-keys
               'now 'years 'months 'days 'hours 'minutes
               'year-start 'month-start 'day-start
               'date 'datetime 'at-zone})

(defn get-fn
  [expression context]
  (cond (seq? expression)
        (let [first-el (first expression)]
          (cond (contains? std-fns first-el) first-el
                (keyword? first-el) :keyword
                (contains? (dissoc context '$) first-el) :user-fn
                :else :implicit-list)) ; ???: list or quote?

        (map? expression) :hash-map

        :else :value))

(defmulti ^:private pl-eval
  #'get-fn)

(defmethod pl-eval :value
  [expression context]
  (cond (and (symbol? expression) (contains? context expression))
        (get context expression)
        
        (symbol? expression) ; ???: should unknown characters throw exceptions?
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
        args (help/flatten-top-level args :keep-empty-lists true)]
    (map #(get % keyword) args)))

(defmethod pl-eval 'list
  [[_ & args] context]
  (map #(pl-eval % context) args))

(defmethod pl-eval :implicit-list
  [expression context]
  (map #(pl-eval % context) expression))

(defmethod pl-eval 'if
  [[_ test t-branch f-branch & rest] context]
  (when (seq rest)
    (throw (Exception. (str "Pathlang syntax exception."
                            "An if expression cannot take more than 3 arguments."))))
  (if (pl-eval test context)
    (pl-eval t-branch context)
    (pl-eval f-branch context)))

(defmethod pl-eval 'count
  [[_ & args] context]
  (apply + (map #(if (seq? %) (count %) 1) args)))

(defn check-and-flatten-args [args & {:keys [ignore-nil]
                                      :or {ignore-nil false}}]
  (let [_ (when (not (help/each-arg-a-val-or-a-single-val-coll? args))
            (throw (Exception. (str "Pathlang syntax exception. "
                                    "Each function argument must be a value or "
                                    "a collection of a single value."))))
        args (help/flatten-top-level args)
        _ (when (< (count args) 2)
            (throw (Exception. (str "Pathlang syntax exception. "
                                    "Function accepts two or more arguments."))))
        _ (when (not (help/same-top-level-type? args :ignore-nil ignore-nil))
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
        args (check-and-flatten-args args)
        arg1 (first args)]
    (cond
      (number? arg1) (apply + args)
      (string? arg1) (apply str args)
      ; ???: date representation
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
        args (check-and-flatten-args args)
        arg1 (first args)]
    (cond
      (number? arg1) (apply - args)
      ; ???: date representation
      :else (throw (Exception. (str "Pathlang syntax exception. "
                                    "The - function supports only numbers and dates."))))))

(defmethod pl-eval '/
  [[_ & args] context]
  (let [args (map #(pl-eval % context) args)
        args (check-and-flatten-args args)
        arg1 (first args)]
    (cond
      (number? arg1) (apply / args)
      ; ???: date representation and what to do with them?
      :else (throw (Exception. (str "Pathlang syntax exception. "
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
  [[_ & args] context]
  nil)

(defmethod pl-eval 'map
  [[_ & args] context]
  nil)

(defmethod pl-eval 'select-keys
  [[_ & args] context]
  nil)

(defmethod pl-eval 'now
  [[_ & args] context]
  nil)

(defmethod pl-eval 'years
  [[_ & args] context]
  nil)

(defmethod pl-eval 'months
  [[_ & args] context]
  nil)

(defmethod pl-eval 'days
  [[_ & args] context]
  nil)

(defmethod pl-eval 'hours
  [[_ & args] context]
  nil)

(defmethod pl-eval 'minutes
  [[_ & args] context]
  nil)

(defmethod pl-eval 'year-start
  [[_ & args] context]
  nil)

(defmethod pl-eval 'month-start
  [[_ & args] context]
  nil)

(defmethod pl-eval 'day-start
  [[_ & args] context]
  nil)

(defmethod pl-eval 'date
  [[_ & args] context]
  nil)

(defmethod pl-eval 'datetime
  [[_ & args] context]
  nil)

(defmethod pl-eval 'at-zone
  [[_ & args] context]
  nil)

(defmethod pl-eval :user-fn
  [[fn-name & args] context]
  nil)

(defmethod pl-eval :default
  [_ _]
  (throw (Exception. "Pathlang syntax exception.")))

(defn evaluate
  "Wrapper over the pl-eval function for argument validation."
  ([expression] (evaluate expression {}))
  ([expression context]
   (when (not (s/valid? ::pls/expression expression))
     (throw (Exception. (str "Pathlang syntax exception in the evaluation expression.\n"
                             (help/beautiful-spec-explain ::pls/expression expression)))))
   (when (not (s/valid? ::pls/context context))
     (throw (Exception. (str "Pathlang syntax exception in the evaluation context.\n"
                             (help/beautiful-spec-explain ::pls/context context)))))
   (pl-eval expression context)))

#_(evaluate 'ext/kek {'$ 42 'ext/kek (fn [a] a)})

#_(defmacro evaluate
  "Wrapper over the pl-eval function for argument validation."
  ([expression] `(evaluate ~expression {}))
  ([expression context]
   (let [context (help/map-value eval context)]
     (when (not (s/valid? ::pls/expression expression))
       (throw (Exception. (str "Pathlang syntax exception in the evaluation expression.\n"
                               (help/beautiful-spec-explain ::pls/expression expression)))))
     (when (not (s/valid? ::pls/context context))
       (throw (Exception. (str "Pathlang syntax exception in the evaluation context.\n"
                               (help/beautiful-spec-explain ::pls/context context)))))
     `(pl-eval '~expression '~context))))

#_(evaluate ext/kek {$ 24 ext/kek (fn [a] a)})
