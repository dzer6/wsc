(ns wiki-stem-corpus.main
  (:require [base.modules.signals :as signals]
            [wiki-stem-corpus.camel.routes.data-ingestion-pipeline]
            [wiki-stem-corpus.camel.routes.dead-letter]
            [wiki-stem-corpus.camel.state]
            [wiki-stem-corpus.langchain.embedding-model]
            [wiki-stem-corpus.langchain.embedding-store]
            [wiki-stem-corpus.migrations]
            [wiki-stem-corpus.server]
            [mount.core :as mount])
  (:gen-class)
  (:import (org.slf4j.bridge SLF4JBridgeHandler)))

(SLF4JBridgeHandler/removeHandlersForRootLogger)
(SLF4JBridgeHandler/install)

(defn -main [& _]
  (signals/add-shutdown-hook (fn []
                               (mount/stop)
                               (shutdown-agents)))
  (mount/start))
