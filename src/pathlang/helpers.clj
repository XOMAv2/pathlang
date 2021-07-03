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

(defn flatten-top-level [coll & {:keys [keep-empty-lists]
                                 :or {keep-empty-lists false}}]
  (mapcat #(cond
             (and keep-empty-lists (sequential? %) (empty? %)) (list ())
             (sequential? %) %
             :else (list %))
          coll))

(defn each-arg-a-val-or-a-single-val-coll? [args]
  (not (some #(and (seq? %) (not= 1 (count %))) args)))

(defn same-top-level-type? [coll & {:keys [ignore-nil]
                                    :or {ignore-nil false}}]
  (let [coll (if ignore-nil (filter some? coll) coll)]
    (apply = (map type coll))))
