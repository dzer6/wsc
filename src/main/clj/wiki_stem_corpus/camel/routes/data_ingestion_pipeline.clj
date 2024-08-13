(ns wiki-stem-corpus.camel.routes.data-ingestion-pipeline
  (:require [base.configuration.api :as config]
            [base.modules.signals :as signals]
            [wiki-stem-corpus.camel.common :as camel-common]
            [wiki-stem-corpus.langchain.embedding-store :as embedding-store]
            [wiki-stem-corpus.camel.endpoint-builder :as endpoint]
            [wiki-stem-corpus.camel.state :as state]
            [wiki-stem-corpus.config :as app-config]
            [clj-camel.core :as c]
            [clojure.tools.logging :as log]
            [mount.core :as mount])
  (:import (java.util Iterator)
           (org.apache.camel.builder Builder)
           (org.apache.camel.dataformat.csv CsvDataFormat)
           (org.apache.camel.model DataFormatDefinition)
           (org.apache.camel.model.dataformat ZipFileDataFormat$Builder)
           (org.apache.commons.csv CSVFormat$Builder)))

(def data-ingestion-pipeline-route-id "data-ingestion-pipeline")

(defn persist-embedding [{:keys [body]}]
  (let [text       (get body "text")
        page-title (get body "page_title")]
    (embedding-store/add "page_title" page-title text)))

(defn zip-data-format []
  (-> (ZipFileDataFormat$Builder.)
      (.usingIterator true)
      (.end)))

(defn csv-data-format [csv-header]
  (DataFormatDefinition. (-> (CsvDataFormat. (-> (CSVFormat$Builder/create)
                                                 (.setSkipHeaderRecord true)
                                                 (.setHeader (into-array String csv-header))
                                                 (.build)))
                             (.setUseMaps true)
                             (.setLazyLoad true))))

(defn enqueue-operation-request-route []
  (c/route-builder (c/from (endpoint/file (config/get @app-config/holder :ingestion-pipeline :source-folder-path)))
                   (c/route-id data-ingestion-pipeline-route-id)

                   (c/unmarshal (zip-data-format))

                   (c/split (Builder/bodyAs Iterator) {:streaming true}

                            (c/unmarshal (csv-data-format ["content_id" "page_title" "section_title" "breadcrumb" "text"]))

                            (c/split (Builder/bodyAs Iterator) {:streaming true}
                                     (c/process persist-embedding)))

                   (c/dead-letter "direct:dead-letter" {:add-exception-message-to-header true})))

;;;

(defonce started? (atom false))

(defn start! []
  (when (and (not @started?) (config/feature-on? @app-config/holder :camel-route :data-ingestion-pipeline))
    (log/infof "Adding `%s` route to Camel context" data-ingestion-pipeline-route-id)

    (signals/run-or-die
      mount/stop
      (c/add-routes @state/context
                    (enqueue-operation-request-route)))
    (reset! started? true)))

(defn stop! []
  (when @started?
    (log/infof "Removing `%s` route from Camel context" data-ingestion-pipeline-route-id)

    (camel-common/shut-down-routes @app-config/holder
                                   @state/context
                                   [data-ingestion-pipeline-route-id])

    (reset! started? false)))

(declare ^:dynamic *routes*)

(mount/defstate ^:dynamic *routes*
  :start (start!)
  :stop (stop!))
