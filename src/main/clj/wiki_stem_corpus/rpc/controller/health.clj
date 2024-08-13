(ns wiki-stem-corpus.rpc.controller.health
  (:require [base.api.error :as error]
            [base.configuration.api :as config]
            [base.utils.function :as uf]
            [clojure.tools.logging :as log]
            [mount.core :as mount]
            [ring.util.http-response :as http]
            [schema.core :as s]
            [taoensso.encore :as enc]
            [wiki-stem-corpus.camel.state :as camel-state]
            [wiki-stem-corpus.config :as app-config]
            [wiki-stem-corpus.db.postgres :as postgres])
  (:import (java.util Optional)
           (org.apache.camel.health HealthCheck$State HealthCheckHelper)))

(defonce get-camel-readiness-holder (atom nil))

(defonce get-camel-liveness-holder (atom nil))

(defonce is-postgres-okay-holder (atom nil))

;;;

(def statuses (s/enum "pass" "fail" "warn"))

;; https://inadarei.github.io/rfc-healthcheck/
(s/defschema HealthCheckResponse
  {:status statuses})

;;;

(defn health-check-result [v]
  (let [state             (.getState v)
        ^Optional message (.getMessage v)
        error             (.getError v)
        details           (.getDetails v)]
    {:state   state
     :message (when (.isPresent message) (.get message))
     :error   (when (.isPresent error) (.get error))
     :details details}))

(defn is-camel-okay [probe-type]
  (let [results (condp = probe-type
                  :liveness (@get-camel-liveness-holder)
                  :readiness (@get-camel-readiness-holder)
                  (error/fire :error-type :resource/server-error
                              :message "Unknown probe type"))]
    (if (every? (comp (uf/equal? HealthCheck$State/UP)
                      :state
                      health-check-result)
                results)
      true
      (do
        (->> results
             (map health-check-result)
             (remove (comp (uf/equal? HealthCheck$State/UP) :state))
             (pr-str)
             (log/warnf "Health checks failed: %s"))
        false))))

(defn readiness-handler []
  (if (and (is-camel-okay :readiness)
           (@is-postgres-okay-holder))
    (http/ok {:status "pass"})
    (http/service-unavailable {:status "fail"})))

(defn liveness-handler []
  (if (and (is-camel-okay :liveness)
           (@is-postgres-okay-holder))
    (http/ok {:status "pass"})
    (http/service-unavailable {:status "fail"})))

;;;

(defonce started? (atom false))

(defn start! []
  (when-not @started?
    (log/info "Initializing Health Controller")
    (let [get-camel-readiness (enc/memoize (config/get @app-config/holder :health :camel :check-period-ms) ; do not check more often than once per X second
                                           (fn []
                                             (try
                                               (HealthCheckHelper/invokeReadiness @camel-state/context)
                                               (catch Exception e
                                                 (log/error e "Error while invoking readiness check")))))
          get-camel-liveness  (enc/memoize (config/get @app-config/holder :health :camel :check-period-ms) ; do not check more often than once per X second
                                           (fn []
                                             (try
                                               (HealthCheckHelper/invokeLiveness @camel-state/context)
                                               (catch Exception e
                                                 (log/error e "Error while invoking liveness check")))))
          is-postgres-okay    (enc/memoize (config/get @app-config/holder :health :postgres :check-period-ms) ; do not check more often than once per X second
                                           postgres/is-connection-alive)]
      (reset! get-camel-readiness-holder get-camel-readiness)
      (reset! get-camel-liveness-holder get-camel-liveness)
      (reset! is-postgres-okay-holder is-postgres-okay)
      (reset! started? true))))

(defn stop! []
  (when @started?
    (log/info "Deinitializing Health Controller")

    (reset! get-camel-readiness-holder nil)
    (reset! started? false)))

(declare ^:dynamic *initializer*)

(mount/defstate ^:dynamic *initializer*
  :start (start!)
  :stop (stop!))
