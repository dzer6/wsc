(ns wiki-stem-corpus.camel.endpoint-builder
  (:import (org.apache.camel.builder.endpoint StaticEndpointBuilders)))

(defn file [path]
  (-> (StaticEndpointBuilders/file path)
      (.advanced)
      (.autoCreate true)))
