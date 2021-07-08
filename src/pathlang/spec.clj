(ns pathlang.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::context
  (s/and (s/map-of
          (s/or :user-fn qualified-symbol?
                :root-el #{'$})
          any?)
         (s/every (fn [[k v]]
                    (or (= '$ k) (fn? v))))))

; The expression must be a function.
(s/def ::expression
  (s/and list?
         (s/every (s/or :pl-value (comp not coll?)
                        :pl-hash-map map?
                        :pl-expression ::expression))))

; An expression can be a function or a value.
#_(s/def ::expression
  (s/or :pl-value (comp not coll?)
        :pl-hash-map map?
        :pl-expression (s/and list?
                              (s/every ::expression))))
