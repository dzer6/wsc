(ns wiki-stem-corpus.migrations
  (:require [mount.core :as mount]
            [base.configuration.api :as config]
            [base.modules.migrations :as migrations]
            [next.jdbc :as jdbc]
            [wiki-stem-corpus.config :as app-config]
            [wiki-stem-corpus.db.postgres :as postgres]))

(defonce migrated? (atom false))

(defn start! []
  (when (and (not @migrated?) (config/feature-on? @app-config/holder :database :run-migrations-on-start))
    (-> @postgres/db
        (jdbc/get-datasource)
        (migrations/migrate))
    (reset! migrated? true)))

(defn stop! []
  (when @migrated?
    (reset! migrated? false)))

(declare ^:dynamic *runner*)

(mount/defstate ^:dynamic *runner*
  :start (start!)
  :stop (stop!))
