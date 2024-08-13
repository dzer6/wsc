(ns wiki-stem-corpus.fixtures
  (:require [base.configuration.api :as config]
            [base.modules.configuration :as core.configuration]
            [base.modules.server :as core.server]
            [clojure.java.io :as io]
            [wiki-stem-corpus.config]
            [wiki-stem-corpus.db.postgres :as postgres]
            [wiki-stem-corpus.migrations]
            [wiki-stem-corpus.server :as web-server]
            [wiki-stem-corpus.test-utils :as tu]
            [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [cprop.source :as cs]
            [mount.core :as mount]
            [next.jdbc :as jdbc])
  (:import (org.apache.commons.io FileUtils)
           (org.testcontainers.containers PostgreSQLContainer)
           (org.testcontainers.containers.output Slf4jLogConsumer)
           (org.testcontainers.utility DockerImageName)))

(def postgres-image "ankane/pgvector:latest")

(defn with-test-config [f]
  (with-redefs [cs/from-env        (fn [] (log/info "[with-test-config] skip ENV vars loading"))
                cs/read-system-env (fn [_] (log/info "[with-test-config] skip system vars loading"))]
    (binding [core.configuration/override-resource-path "test-config.edn"]
      (log/info "[with-test-config]")
      (try
        (mount/start #'wiki-stem-corpus.config/*loader*)
        (f)
        (catch Exception e
          (do-report {:type     :error
                      :message  "Uncaught exception, not in assertion."
                      :expected nil
                      :actual   e}))
        (finally
          (mount/stop #'wiki-stem-corpus.config/*loader*))))))

(defn with-web-server [app-config]
  (fn [f]
    (log/info "[with-web-server]")
    (let [http-server-holder (atom nil)
          port               (tu/get-free-port)]
      (try
        (swap! app-config assoc-in [:web :server :host] "localhost")
        (swap! app-config assoc-in [:web :server :port] port)
        (core.server/start-server! http-server-holder
                                   (web-server/http-handler @app-config)
                                   @app-config)
        (f)
        (catch Exception e
          (do-report {:type     :error
                      :message  "Uncaught exception, not in assertion."
                      :expected nil
                      :actual   e}))
        (finally
          (core.server/stop-server! http-server-holder))))))

(defn with-postgres [app-config]
  (fn [f]
    (log/info "[with-postgres]")
    (let [log-consumer (Slf4jLogConsumer. (.get-logger log/*logger-factory* "postgres-container"))
          container    (-> postgres-image
                           (DockerImageName/parse)
                           (.asCompatibleSubstituteFor "postgres")
                           (PostgreSQLContainer.))]
      (try
        (.start container)
        (.followOutput container log-consumer)
        (swap! app-config assoc-in [:postgres :server-name] (.getContainerIpAddress container)) ; TODO Refactor
        (swap! app-config assoc-in [:postgres :port-number] (.getMappedPort container 5432))
        (mount/start #'wiki-stem-corpus.db.postgres/*database*)
        (f)
        (catch Exception e
          (do-report {:type     :error
                      :message  "Uncaught exception, not in assertion."
                      :expected nil
                      :actual   e}))
        (finally
          (mount/stop #'wiki-stem-corpus.db.postgres/*database*)
          (.stop container))))))

(defn with-postgres-migrations [f]
  (log/info "[with-postgres-migrations]")
  (try
    (mount/start #'wiki-stem-corpus.migrations/*runner*)
    (f)
    (catch Exception e
      (do-report {:type     :error
                  :message  "Uncaught exception, not in assertion."
                  :expected nil
                  :actual   e}))
    (finally
      (mount/stop #'wiki-stem-corpus.migrations/*runner*))))

(defn with-postgres-seeding [seeding-file-path]
  (fn [f]
    (try
      (log/infof "[with-postgres-seeding] sql file path: %s" seeding-file-path)
      (->> (format seeding-file-path)
           (slurp)
           (vector)
           (jdbc/execute! @postgres/db))
      (f)
      (catch Exception e
        (do-report {:type     :error
                    :message  "Uncaught exception, not in assertion."
                    :expected nil
                    :actual   e})))))

(defn with-camel-context [f]
  (log/info "[with-camel-context]")
  (try
    (mount/start #'wiki-stem-corpus.camel.state/*context*)
    (f)
    (catch Exception e
      (do-report {:type     :error
                  :message  "Uncaught exception, not in assertion."
                  :expected nil
                  :actual   e}))
    (finally
      (mount/stop #'wiki-stem-corpus.camel.state/*context*))))

(defn with-camel-routes [& routes]
  (fn [f]
    (log/info "[with-camel-routes]")
    (try
      (doseq [r routes]
        (mount/start r))
      (f)
      (catch Exception e
        (do-report {:type     :error
                    :message  "Uncaught exception, not in assertion."
                    :expected nil
                    :actual   e}))
      (finally
        (doseq [r routes]
          (mount/stop r))))))

(defn with-file-for-ingestion [app-config path]
  (fn [f]
    (log/info "[with-file-for-ingestion]")
    (FileUtils/copyFileToDirectory (io/file path)
                                   (io/file (config/get @app-config :ingestion-pipeline :source-folder-path)))
    (try
      (f)
      (catch Exception e
        (do-report {:type     :error
                    :message  "Uncaught exception, not in assertion."
                    :expected nil
                    :actual   e})))))