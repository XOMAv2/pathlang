(ns pathlang.spec
  (:require [clojure.spec.alpha :as s]
            [pathlang.helpers :refer [atomic-value?
                                      atomic-single-value-coll?]]))

(s/def ::context
  (s/and (s/map-of
          (s/or :user-fn qualified-symbol?
                :root-el #{'$})
          any?)
         (s/every (fn [[k v]]
                    (or (= '$ k) (fn? v))))))

#_"The expression must be a function."
(s/def ::expression
  (s/and list?
         (s/every (s/or :pl-value (comp not coll?)
                        :pl-hash-map map?
                        :pl-expression ::expression))))

#_"An expression can be a function or a value."
#_(s/def ::expression
  (s/or :pl-value (comp not coll?)
        :pl-hash-map map?
        :pl-expression (s/and list?
                              (s/every ::expression))))

(s/def ::atomic-val-1
  (s/or :atomic-value atomic-value?
        :atomic-single-value-coll atomic-single-value-coll?))

(s/def ::atomic-val-0-1
  (s/or :atomic-value atomic-value?
        :atomic-single-value-coll atomic-single-value-coll?
        :empty-coll (s/and coll? empty?)))

(s/def ::atomic-val-0+
  (s/or :atomic-value atomic-value?
        :atomic-value-coll (s/coll-of atomic-value?)))

(s/def ::map-val-0+
  (s/or :hash-map map?
        :hash-map-coll (s/coll-of map?)))
