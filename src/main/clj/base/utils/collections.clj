(ns base.utils.collections
  (:import (java.util Properties)))

(defn deep-merge
  "Merge several maps into one with superset of fields. Latest wins"
  [a & maps]
  (if (map? a)
    (apply merge-with deep-merge a maps)
    (apply merge-with deep-merge maps)))

(defn map->properties
  "Converts a map to java.util.Properties
   Nested keys will result in dot separated path (a.b.c)
   For clojure keywords name fn applied, otherwise str"
  [m]
  (letfn [(val [v] (if (keyword? v) (name v) (str v)))
          (key [prefix k] (if prefix (str prefix "." (val k)) (val k)))
          (traverse [prefix m]
            (mapcat (fn [[k v]]
                      (let [path (key prefix k)]
                        (if (map? v)
                          (traverse path v)
                          [path (val v)])))
                    m))]
    (let [properties (Properties.)
          pairs      (->> m
                          (traverse nil)
                          (partition 2 2))]
      (doseq [[k v] pairs]
        (.put properties k v))

      properties)))