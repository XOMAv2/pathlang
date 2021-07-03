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
  (if (contains? context expression)
    (get context expression)
    expression)) ; ???: what to do with unknown symbols?

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
                            "An if expression cannot take more than 3 arguments"))))
  (if (pl-eval test context)
    (pl-eval t-branch context)
    (pl-eval f-branch context)))

(defmethod pl-eval 'count
  [[_ & args] context]
  (apply + (map #(if (seq? %) (count %) 1) args)))

(defmethod pl-eval '=
  [[_ & args] context]
  nil)

(defmethod pl-eval 'not=
  [[_ & args] context]
  nil)

(defmethod pl-eval '>
  [[_ & args] context]
  nil)

(defmethod pl-eval '<
  [[_ & args] context]
  nil)

(defmethod pl-eval '>=
  [[_ & args] context]
  nil)

(defmethod pl-eval '<=
  [[_ & args] context]
  nil)

(defmethod pl-eval '+
  [[_ & args] context]
  nil)

(defmethod pl-eval '*
  [[_ & args] context]
  nil)

(defmethod pl-eval '-
  [[_ & args] context]
  nil)

(defmethod pl-eval '/
  [[_ & args] context]
  nil)

(defmethod pl-eval 'sum
  [[_ & args] context]
  nil)

(defmethod pl-eval 'product
  [[_ & args] context]
  nil)

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
