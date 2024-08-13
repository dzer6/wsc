(ns wiki-stem-corpus.camel.common
  (:require [base.configuration.api :as config]
            [clj-camel.core]
            [clj-camel.core :as c]
            [clojure.tools.logging :as log])
  (:import (java.util.concurrent TimeUnit)
           (org.apache.camel.impl DefaultCamelContext)))

(defn shut-down-routes [config ^DefaultCamelContext camel-context route-ids]
  (try
    (if (config/get config :dev-mode)
      (pmap (partial c/stop-and-remove-route camel-context 1 TimeUnit/SECONDS) route-ids)
      (mapv (partial c/stop-and-remove-route camel-context) route-ids))
    (catch Exception e
      (log/error e "Unable to remove routes from Camel context"))))