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

(defn key-intersection
  ([m1] (keys m1))
  ([m1 m2] (->> (keys m1)
                (select-keys m2)
                (keys)))
  ([m1 m2 & ms] (reduce (fn [keyseq map]
                          (keys (select-keys map keyseq)))
                        (keys m1)
                        (cons m2 ms))))
