(ns pathlang.helpers
  (:require [expound.alpha :as ex]))

(defn not-coll?
  "Returns true if x does not implements IPersistentCollection."
  [x]
  (not (coll? x)))

(defn map-value [f map]
  (reduce-kv (fn [acc k v]
               (assoc acc k (f v)))
             {} map))

(defn beautiful-spec-explain
  "Just a wrapper over the expound-str function of
   the expound library with several keys."
  [spec val]
  (ex/expound-str spec val
                  {:print-specs? false
                   :show-valid-values? true}))

