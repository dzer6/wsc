(ns wiki-stem-corpus.camel.state
  (:require [base.configuration.api :as config]
            [base.modules.signals :as signals]
            [base.utils.collections :as collections]
            [clj-camel.util :as cu]
            [clojure.tools.logging :as log]
            [mount.core :as mount]
            [wiki-stem-corpus.config :as app-config]
            [wiki-stem-corpus.migrations])
  (:import (clojure.lang APersistentMap)
           (java.io InputStream)
           (java.util Map)
           (org.apache.camel CamelContext Exchange)
           (org.apache.camel.impl DefaultCamelContext)
           (org.apache.camel.impl.debugger DefaultDebugger)
           (org.apache.camel.impl.health DefaultHealthCheckRegistry RoutesHealthCheckRepository)
           (org.apache.camel.spi PropertiesComponent)))

;;;

(defonce context (atom nil))
(defonce components-registry (atom nil))

;;;

(defn build-context []
  (let [context                                   (DefaultCamelContext.)
        type-converter-registry                   (.getTypeConverterRegistry context)
        default-registry                          (.getRegistry context)
        ^PropertiesComponent properties-component (.getPropertiesComponent context)
        health-check-registry                     (DefaultHealthCheckRegistry.)]
    (reset! components-registry default-registry)

    (.setStreamCaching context false)

    (.setUseMDCLogging context true)

    (->> (collections/map->properties @app-config/holder)
         (.setInitialProperties properties-component))

    (.addTypeConverter type-converter-registry
                       InputStream
                       APersistentMap
                       cu/map-to-input-stream-converter)

    (.addTypeConverter type-converter-registry
                       Map
                       Exchange
                       cu/exchange->map)

    (.addTypeConverter type-converter-registry
                       InputStream
                       Map
                       cu/map-to-input-stream-converter)

    (.setLoadHealthChecks context true)
    (.register health-check-registry (RoutesHealthCheckRepository.))

    (when (config/feature-on? @app-config/holder :debugger :camel)
      (.setDebugger context (DefaultDebugger.)))

    (when (config/feature-on? @app-config/holder :logging :camel-tracer)
      (.setTracingStandby context true)
      (.setTracing context true))

    context))

;;;

(defn start! []
  (when-not @context
    (log/info "Starting Camel context...")
    (->> (build-context)
         (reset! context)
         (.start))))

(defn stop! []
  (when-let [^CamelContext ctx @context]
    (log/info "Stopping Camel context...")
    (.shutdown ctx)
    (reset! context nil)))

(declare ^:dynamic *context*)

(mount/defstate ^:dynamic *context*
  :start (signals/run-or-die mount/stop
                             (start!))
  :stop (stop!))
