(ns wiki-stem-corpus.config
  (:require [base.configuration.common :as common]
            [base.modules.configuration :as config]
            [base.utils.collections :as collections]
            [mount.core :as mount]
            [schema.core :as s]))

(def cors {:cors {:allowed-origins s/Str
                  :allowed-headers s/Str}})

(def camel {:feature {:debugger    {:camel common/TurnOn}
                      :logging     {:camel-tracer common/TurnOn}
                      :camel-route {:data-ingestion-pipeline common/TurnOn}}})

(def health {:health {:postgres {:check-period-ms s/Int}
                      :camel    {:check-period-ms s/Int}}})

(def llm {:langchain {:embedding-model {:url  s/Str
                                        :name s/Str}
                      :embedding-store {:dimension s/Int}}
          :feature   {:langchain {:embedding-model common/TurnOn
                                  :embedding-store common/TurnOn}}})

(def app {:ingestion-pipeline {:source-folder-path s/Str}})

(s/defschema Configuration
  (collections/deep-merge common/rpc
                          common/dev-mode
                          common/postgres
                          cors
                          camel
                          health
                          llm
                          app))

(defonce holder (atom {}))

(def hide-passwords-key-names #{:pass :password :secret})

(declare ^:dynamic *loader*)

(mount/defstate ^:dynamic *loader*
  :start (config/load! mount/stop holder hide-passwords-key-names Configuration)
  :stop (config/stop! holder))
