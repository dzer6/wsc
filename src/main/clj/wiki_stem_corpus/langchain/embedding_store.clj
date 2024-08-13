(ns wiki-stem-corpus.langchain.embedding-store
  (:require [base.configuration.api :as config]
            [wiki-stem-corpus.config :as app-config]
            [wiki-stem-corpus.langchain.embedding-model :as embedding-model]
            [clojure.tools.logging :as log]
            [mount.core :as mount])
  (:import (dev.langchain4j.data.document Metadata)
           (dev.langchain4j.data.embedding Embedding)
           (dev.langchain4j.data.segment TextSegment)
           (dev.langchain4j.model.embedding EmbeddingModel)
           (dev.langchain4j.model.output Response)
           (dev.langchain4j.store.embedding EmbeddingSearchRequest EmbeddingStore)
           (dev.langchain4j.store.embedding.pgvector DefaultMetadataStorageConfig MetadataStorageMode PgVectorEmbeddingStore)))

(defonce holder (atom nil))

;;;

(defn add [^String metadata-key ^String metadata-val ^String text]
  (let [^TextSegment segment (TextSegment. text (Metadata/from metadata-key metadata-val))
        ^Response response   (some-> ^EmbeddingModel @embedding-model/holder
                                     (.embed segment))
        ^Embedding embedding (.content response)]
    (some-> ^EmbeddingStore @holder
            (.add embedding
                  segment))))

(defn search [text max-results min-score metadata-filter]
  (let [^TextSegment segment (TextSegment/from text)
        ^Response response   (some-> ^EmbeddingModel @embedding-model/holder
                                     (.embed segment))
        ^Embedding embedding (.content response)
        request              (EmbeddingSearchRequest. embedding max-results min-score metadata-filter)]
    (some-> ^EmbeddingStore @holder
            (.search request)
            (.matches))))

;;;

(defn start! []
  (when (and (nil? @holder) (config/feature-on? @app-config/holder :langchain :embedding-store))
    (log/info "Starting Langchain Embedding Store")
    (try
      (->> (-> (PgVectorEmbeddingStore/builder)
               (.host (config/get @app-config/holder :postgres :server-name))
               (.port (some-> (config/get @app-config/holder :postgres :port-number)
                              (int)))
               (.database (config/get @app-config/holder :postgres :database-name))
               (.user (config/get @app-config/holder :postgres :username))
               (.password (config/get @app-config/holder :postgres :password))
               (.table "wiki_stem_corpus")
               (.createTable false)
               (.dimension (some-> (config/get @app-config/holder :langchain :embedding-store :dimension)
                                   (int)))
               (.metadataStorageConfig (-> (DefaultMetadataStorageConfig/builder)
                                           (.storageMode MetadataStorageMode/COMBINED_JSONB)
                                           (.columnDefinitions ["metadata JSONB NULL"])
                                           (.build)))
               (.build))
           (reset! holder))
      (catch Exception e
        (log/error e)))))

(defn stop! []
  (when (some? @holder)
    (log/info "Stopping Langchain Embedding Store")

    (reset! holder nil)))

(declare ^:dynamic *runner*)

(mount/defstate ^:dynamic *runner*
  :start (start!)
  :stop (stop!))
