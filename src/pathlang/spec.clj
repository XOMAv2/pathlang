(ns pathlang.spec
  (:require [clojure.spec.alpha :as s]
            [pathlang.helpers :refer [not-coll?]]))

(s/def ::context
  (s/and (s/map-of
          (s/or :user-fn qualified-symbol?
                :root-el #{'$})
          any?)
         (s/every (fn [[k v]]
                    (or (= '$ k) (fn? v))))))

; The expression must be a function.
#_(s/def ::expression
  (s/and list?
         (s/every (s/or :pl-value not-coll?
                        :pl-expression ::expression))))

; An expression can be a function or a value.
(s/def ::expression
  (s/or :pl-value not-coll?
        :pl-expression (s/and list?
                              (s/every ::expression))))