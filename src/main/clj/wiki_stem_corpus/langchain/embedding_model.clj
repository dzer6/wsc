(ns wiki-stem-corpus.langchain.embedding-model
  (:require [base.configuration.api :as config]
            [wiki-stem-corpus.config :as app-config]
            [clojure.tools.logging :as log]
            [mount.core :as mount])
  (:import (dev.langchain4j.data.segment TextSegment)
           (dev.langchain4j.model.ollama OllamaEmbeddingModel)))

(defonce holder (atom nil))

;;;

(defn embed [text]
  (some-> @holder
          (.embed (TextSegment/from text))))

;;;

(defn start! []
  (when (and (nil? @holder) (config/feature-on? @app-config/holder :langchain :embedding-model))
    (log/info "Starting Langchain Embedding Model")
    (try
      (->> (-> (OllamaEmbeddingModel/builder)
               (.baseUrl (config/get @app-config/holder :langchain :embedding-model :url))
               (.modelName (config/get @app-config/holder :langchain :embedding-model :name))
               (.build))
           (reset! holder))
      (catch Exception e
        (log/error e)))))

(defn stop! []
  (when (some? @holder)
    (log/info "Stopping Langchain Embedding Model")
    (reset! holder nil)))

(declare ^:dynamic *runner*)

(mount/defstate ^:dynamic *runner*
  :start (start!)
  :stop (stop!))