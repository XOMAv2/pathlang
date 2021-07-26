(ns pathlang.stdlib.utils
  #?(:cljs (:require-macros [pathlang.stdlib.utils :refer [check-arg]]))
  (:require [pathlang.helpers :as help :refer [date? atomic-value?]]
            [#?(:clj clojure.spec.alpha :cljs cljs.spec.alpha) :as s]
            #_[clojure.string]))

(def logical-false #{false () #{} {} nil})

(defn logical-false? [x]
  (when (not (or (atomic-value? x)
                 (empty? x)
                 (help/atomic-single-value-coll? x)))
    (throw (ex-info (str "Implicit conversion to logical true or false is "
                         "available only for atomic values, empty "
                         "collections and single atomic value collections.")
                    {:cause :pathlang-implicit-logical-conversion
                     :value x})))
  (contains? logical-false x))

(defn logical-true? [x]
  (not (logical-false? x)))

(defn check-types
  "If type-checkers collection is provided, then for one of the checkers each
   argument must return true value.
   For date checking it is required to use the pathlang.helpers/date? predicate."
  [called-f args & {:keys [ignore-nil type-checkers]
                    :or {ignore-nil false
                         type-checkers #{any?}}}]
  (let [args (if ignore-nil (filter some? args) args)
        type-checkers (set type-checkers)]
    (when (not (or (and (help/same-top-level-type? args)
                        (->> (first args)
                             ((apply juxt type-checkers))
                             (some identity)))
                   (and (type-checkers date?)
                        (date? (first args)))))
      (throw (ex-info (str "Not all atomic values extracted from function "
                           (help/fn-name called-f) " arguments have the same type.")
                      {:cause :pathlang-type-mismatch
                       :called-function called-f
                       :type-checkers type-checkers
                       :ignore-nil ignore-nil
                       :same-top-level-type? (help/same-top-level-type? args)
                       :arg-types (map type args)
                       :args args}))))
  args)

#?(:clj
   (defmacro check-arg
     [s-valid? called-f spec arg-name arg-value & {:keys [ex-msg]}]
     `(when (not (~s-valid? ~spec ~arg-value))
        (throw (ex-info ~(or ex-msg
                             `(str "Argument " ~arg-name " with value "
                                   ~arg-value " does not satisfy the constraints of "
                                   "the function " (help/fn-name ~called-f) "."))
                        {:cause :pathlang-arg-constraints
                         :called-function ~called-f
                         :arg-name ~arg-name
                         :arg-value ~arg-value
                         :spec '~spec
                         :spec-explain (help/beautiful-spec-explain ~spec ~arg-value)})))))

#_(defn pl-fn-name [f]
    (-> (help/fn-name f)
        (clojure.string/replace
         (re-pattern (str "^" (ns-name *ns*) "/pl-"))
         (str (ns-name *ns*) \/))))

(defn make-constraint
  ([spec arg-count variadic?]
   (make-constraint (repeat arg-count spec) (when variadic? spec)))
  ([specs]
   (make-constraint specs nil))
  ([specs variadic-spec]
   (fn [f & args]
     #_"Arg count check."
     (when (or (< (count args) (count specs))
               (and (nil? variadic-spec) (> (count args) (count specs))))
       (throw (ex-info (str "Wrong number of args (" (count args) ") passed to: "
                            (help/fn-name f) ".")
                       {:cause :function-arity-mismatch
                        :called-function f
                        :function-fixed-arity (count specs)
                        :variadic? (nil? variadic-spec)
                        :given-arg-count (count args)})))

     #_"Fixed args validation."
     (doall
      (map (fn [index spec arg-value]
             (let [arg-name (symbol (str "arg" index))]
               (check-arg s/valid? f spec arg-name arg-value)))
           (range 1 (inc (count specs))) specs args))

     #_"Variadic args validation."
     (when variadic-spec
       (let [args (doall (drop (count specs) args))]
         (check-arg s/valid? f (s/coll-of variadic-spec) 'args args)))

     #_"Function applying."
     (apply f args))))

#_(defmacro make-constraint
    ([spec arg-count variadic?]
     `(make-constraint ~(repeat arg-count spec) ~(when variadic? spec)))
    ([specs]
     `(make-constraint ~specs nil))
    ([specs variadic-spec]
     (let [f (gensym "f")
           args (gensym "args")]
       `(fn [~f & ~args]
          #_"Arg count check."
          (when (or (< (count ~args) ~(count specs))
                    (and ~(nil? variadic-spec) (> (count ~args) ~(count specs))))
            (throw (ex-info (str "Wrong number of args (" (count ~args) ") passed to: "
                                 (help/fn-name ~f) ".")
                            {:cause :function-arity-mismatch
                             :called-function ~f
                             :function-fixed-arity ~(count specs)
                             :variadic? ~(nil? variadic-spec)
                             :given-arg-count (count ~args)})))

          #_"Fixed args validation."
          #_(doall
             (map (fn [index# spec# arg-value#]
                    (let [arg-name# (symbol (str "arg" (inc index#)))]
                      (check-arg s/valid? ~f spec# arg-name# arg-value#)))
                  (range ~(count specs)) ~specs ~args))
          ~@(map (fn [index spec]
                   (let [arg-name (symbol (str "arg" (inc index)))]
                     `(check-arg s/valid? ~f ~spec '~arg-name (nth ~args ~index))))
                 (range (count specs)) specs)

          #_"Variadic args validation."
          (when ~variadic-spec
            (let [~args (drop ~(count specs) ~args)]
              (check-arg s/valid? ~f (s/coll-of ~variadic-spec) 'args ~args)))

          #_"Function applying."
          (apply ~f ~args)))))
