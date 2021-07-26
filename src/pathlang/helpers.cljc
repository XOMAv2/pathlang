(ns pathlang.helpers
  (:require [expound.alpha :as ex]
            #?(:clj [clojure.repl :refer [demunge]])
            #?(:cljs [clojure.string])))

(defn atomic-value?
  "In terms of pathlang an atomic value is any non-collection or map."
  [x]
  (or (map? x) (-> x coll? not)))

(defn single-value-coll?
  "Note that map is not a collection in pathlang terminology."
  [x]
  (and (-> x atomic-value? not)
       (= 1 (count x))))

(defn atomic-single-value-coll?
  "Note that map is not a collection in pathlang terminology."
  [x]
  (and (-> x atomic-value? not)
       (= 1 (count x))
       (-> x first atomic-value?)))

(defn beautiful-spec-explain
  "Just a wrapper over the expound-str function of
   the expound library with several keys."
  [spec val]
  (ex/expound-str spec val
                  {:print-specs? false
                   :show-valid-values? true}))

(defn flatten-top-level [coll & {:keys [keep-empty-lists]
                                 :or {keep-empty-lists false}}]
  (mapcat #(cond
             (and keep-empty-lists (-> % atomic-value? not) (empty? %)) (list ())
             (-> % atomic-value? not) %
             :else (list %))
          coll))

(defn every-arg-by-some-pred
  "Returns true if any of its predicates return a logical true value
   against all of its arguments, else it returns false.
   Returns true for empty collections of args.
   Predicates are applied to the argument until a logical true is obtained
   (like `or`-composition of preds)."
  [args & preds]
  (let [preds (or preds [any?])]
    (every? (apply some-fn preds) args)))

(defn same-top-level-type? [coll & {:keys [ignore-nil]
                                    :or {ignore-nil false}}]
  (let [coll (if ignore-nil (filter some? coll) coll)]
    (apply = (map type coll))))

(defn fn-name
  #_"https://stackoverflow.com/questions/22116257/how-to-get-functions-name-as-string-in-clojure"
  [f]
  #?(:clj (as-> (str f) $
            (demunge $)
            (or (re-find #"(.+)--\d+@" $)
                (re-find #"(.+)@" $))
            (last $))
     :cljs (as-> (.-name f) $
             (demunge $)
             (clojure.string/split $ #"/")
             ((juxt butlast last) $)
             (update $ 0 #(clojure.string/join "." %))
             (clojure.string/join "/" $))))

(defn date? [x]
  (instance? #?(:clj java.util.Date :cljs js/Date) x))

(defn contains?-update
  ([m k f]
   (if (contains? m k) (update m k f) m))
  ([m k f x]
   (if (contains? m k) (update m k f x) m))
  ([m k f x y]
   (if (contains? m k) (update m k f x y) m))
  ([m k f x y z]
   (if (contains? m k) (update m k f x y z) m))
  ([m k f x y z & more]
   (if (contains? m k) (apply update m k f x y z more) m)))
