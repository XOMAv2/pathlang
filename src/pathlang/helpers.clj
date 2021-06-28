(ns pathlang.helpers)

(defn not-coll?
  "Returns true if x does not implements IPersistentCollection."
  [x]
  (not (coll? x)))
