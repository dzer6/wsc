(ns wiki-stem-corpus.rpc.controller.langchain
  (:require [base.api.error :as error]
            [base.modules.database-consistency :as database-consistency]
            [next.jdbc :as jdbc]
            [wiki-stem-corpus.db.postgres :as postgres]
            [wiki-stem-corpus.db.query :as query]
            [wiki-stem-corpus.langchain.embedding-store :as embedding-store]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [schema.core :as s]
            [slingshot.slingshot :refer [try+]])
  (:import (dev.langchain4j.store.embedding EmbeddingMatch)))

(s/defschema SearchResponse
  [{:score    Double
    :text     s/Str
    :metadata {s/Str s/Str}}])

(defn search [text max-results min-score]
  (log/infof "search, text: %s, max-results: %s, min-score: %s" text max-results min-score)
  (when (string/blank? text)
    (error/fire :error-type :resource/bad-request
                :message "Wrong input. The parameter `text` should not be blank."))
  (when (or (< max-results 1) (> max-results 100))
    (error/fire :error-type :resource/bad-request
                :message "Wrong input. The parameter `max-results` should be between 1 and 100."))
  (when (or (< min-score 0.0) (> min-score 1.0))
    (error/fire :error-type :resource/bad-request
                :message "Wrong input. The parameter `min-score` should be between 0.0 and 1.0."))
  (try+
    (let [res (some->> (embedding-store/search text (int max-results) min-score nil)
                       (mapv (fn [^EmbeddingMatch embedding-match]
                               {:score    (.score embedding-match)
                                :text     (some-> embedding-match
                                                  (.embedded)
                                                  (.text))
                                :metadata (some->> embedding-match
                                                   (.embedded)
                                                   (.metadata)
                                                   (.toMap)
                                                   (into {}))})))]
      (log/infof "search result: %s" res)
      res)
    (catch Exception e
      (log/error e)
      (error/fire :error-type :resource/bad-request
                  :cause e
                  :message "Unable to perform langchain search."))))

;;;

(s/defschema WikiStemCorpusMetadataResponse
  {:ingested-items-count s/Num})

(defn metadata []
  (log/info "obtain Wiki STEM Corpus metadata")
  (try+
    (database-consistency/wrap-exceptions
      (jdbc/with-transaction [tx @postgres/db]
        {:ingested-items-count (:count (query/count-wiki-stem-items tx {}))}))
    (catch Exception e
      (log/error e)
      (error/fire :error-type :resource/bad-request
                  :cause e
                  :message "Unable to obtain past evaluations."))))