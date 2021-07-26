(ns pathlang.interpreter
  (:require [#?(:clj clojure.spec.alpha :cljs cljs.spec.alpha) :as s]
            [pathlang.helpers :as help :refer [atomic-value?]]
            #?(:cljs [cljs.reader :refer [read-string]])
            [pathlang.stdlib :as std]
            [#?(:clj clojure.core.match :cljs cljs.core.match) :refer [match]]
            [pathlang.spec :as pls]))

(defn get-fn
  [expression context]
  (let [result (cond
                 (map? expression) :hash-map
                 (atomic-value? expression) :value
                 (keyword? (first expression)) :keyword
                 (contains? (dissoc context '$) (first expression)) :fn
                 :else :implicit-list)]
    #_(println [result expression])
    result))

(defmulti ^:private pl-eval
  #'get-fn)

(defmethod pl-eval :value
  [expression context]
  (cond (and (symbol? expression) (contains? context expression))
        (get context expression)
        
        (symbol? expression)
        (throw (ex-info (str "Pathlang interpreter cannot resolve symbol "
                             expression
                             " in this context.")
                        {:cause :pathlang-interpreter-symbol-evaluation
                         :expression expression
                         :context context}))
        
        :else expression))

(defmethod pl-eval :hash-map
  [expression context]
  (->> expression
       (map (fn [[k v]]
              [(pl-eval k context) (pl-eval v context)]))
       (into {})))

(defmethod pl-eval :implicit-list
  [args context]
  (->> (map #(pl-eval % context) args)
       (apply (pl-eval 'list context)))
  #_"Seceond solution lead to stackoverflow exception if empty context is passed."
  #_(pl-eval (cons 'list args) context))

(defmethod pl-eval :keyword
  [args context]
  (->> (map #(pl-eval % context) args)
       (apply (pl-eval 'keyword context))))

(defn- assoc-% [context value]
  (let [context (update context :lambda-nesting inc)
        lambda-nesting (:lambda-nesting context)
        lambda-sym (symbol (str "%" lambda-nesting))
        context (assoc context lambda-sym value)]
    (if (= 1 lambda-nesting)
        (assoc context '% value)
        context)
    #_"To enable interpretation of the % symbol as an argument of the
       most nested lambda function, you need to uncomment the following form
       and ignore the previous."
    #_(assoc context '% value)))

(defn eval-args
  "If :fn-indexes key is set to true, the arguments with the specified indexes will 
   be wrapped into lambda expressions to evaluate them on demand.
   :fn-indexes can be nil, set of indexes or :all keyword.
   If :fn-take-arg? key is set to true, the created lambda will take one argument, 
   which will be associated to the context by the key '%."
  [args context & {:keys [fn-indexes fn-take-arg?]}]
  (match [fn-indexes fn-take-arg?]
    [nil _]     (map (fn [arg] (pl-eval arg context)) args)
    [:all true] (map (fn [arg] (fn [curr] (pl-eval arg (assoc-% context curr)))) args)
    [:all _]    (map (fn [arg] #(pl-eval arg context)) args)
    :else       (map-indexed (fn [index arg]
                               (match [(contains? fn-indexes index) fn-take-arg?]
                                 [true true] (fn [curr] (pl-eval arg (assoc-% context curr)))
                                 [true _] #(pl-eval arg context)
                                 :else (pl-eval arg context)))
                             args)))

(defmethod pl-eval :fn
  [[fn-name & args] context]
  (let [args (match fn-name
               'if (eval-args args context :fn-indexes #{1 2})

               (:or 'or 'and)
               (eval-args args context :fn-indexes :all)
               
               (:or 'filter 'map 'select-keys)
               (eval-args args context :fn-indexes #{0} :fn-take-arg? true)

               :else (eval-args args context))
        fn-name (pl-eval fn-name context)]
    (apply fn-name args)))

(defmethod pl-eval :default
  [expression context]
  (throw (ex-info (str "Pathlang interpreter does not provide "
                       "method for dispatched expression " expression ".")
                  {:cause :pathlang-interpreter-dispatching
                   :expression expression
                   :context context})))

(defn evaluate
  "Wrapper over the pl-eval function for argument validation 
   and providing standard functions context."
  ([expression] (evaluate expression {}))
  ([expression context]
   (let [expression (read-string expression)]
     (when (not (s/valid? ::pls/expression expression))
       (throw (throw (ex-info "Pathlang syntax exception in the evaluation expression."
                              {:cause :pathlang-syntax
                               :called-function evaluate
                               :arg-value expression
                               :spec ::pls/expression
                               :spec-explain (help/beautiful-spec-explain
                                              ::pls/expression expression)}))))
     (when (not (s/valid? ::pls/context context))
       (throw (throw (ex-info "Pathlang syntax exception in the evaluation context."
                              {:cause :pathlang-syntax
                               :called-function evaluate
                               :arg-value context
                               :spec ::pls/context
                               :spec-explain (help/beautiful-spec-explain
                                              ::pls/context context)}))))
     (let [context (into context std/fns)
           context (assoc context :lambda-nesting 0)
           result (pl-eval expression context)]
       (if (seq? result)
         (doall result)
         result)))))

#_(evaluate "((:key {:key 1}) (ext/kek $))" {'$ 42 'ext/kek (fn [a] a)})
