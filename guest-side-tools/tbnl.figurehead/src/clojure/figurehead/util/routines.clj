(ns figurehead.util.routines)

(declare vector-to-map)

(defn vector-to-map
  "convert [:key1 val1 :key2 val2 ...] into {:key1 val1, key2 val2, ...}"
  [v]
  (into {} (map vec (partition 2 v))))

