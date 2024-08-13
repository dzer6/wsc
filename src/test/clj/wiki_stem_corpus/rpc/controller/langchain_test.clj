(ns wiki-stem-corpus.rpc.controller.langchain-test
  (:require [spy.core :as spy]
            [wiki-stem-corpus.config :as app-config]
            [wiki-stem-corpus.fixtures :as fixtures]
            [wiki-stem-corpus.langchain.embedding-store :as embedding-store]
            [wiki-stem-corpus.test-utils :as tu]
            [clojure.test :refer :all])
  (:import (dev.langchain4j.data.document Metadata)
           (dev.langchain4j.data.embedding Embedding)
           (dev.langchain4j.data.segment TextSegment)
           (dev.langchain4j.store.embedding EmbeddingMatch)))

(->> [fixtures/with-test-config
      (fixtures/with-postgres app-config/holder)
      fixtures/with-postgres-migrations
      (fixtures/with-postgres-seeding "src/test/resources/data/embeddings.sql")
      (fixtures/with-web-server app-config/holder)]
     (join-fixtures)
     (use-fixtures :once))

(defn perform-searching [query-params]
  (tu/make-http-get-call app-config/holder
                         "search"
                         query-params))

(deftest documents-searching
  (testing "correctness of documents searching in vector database, error response on zero `max-results` param value"
    (let [query-params  {:text        "some textual explanation of a term related to wiki article"
                         :max-results 0
                         :min-score   0.6}
          http-response (perform-searching query-params)]
      (is (= 400 (:status http-response)))))

  (testing "correctness of documents searching in vector database, error response on large `max-results` param value"
    (let [query-params  {:text        "some textual explanation of a term related to wiki article"
                         :max-results 1000
                         :min-score   0.6}
          http-response (perform-searching query-params)]
      (is (= 400 (:status http-response)))))

  (testing "correctness of documents searching in vector database, error response on negative `min-score` param value"
    (let [query-params  {:text        "some textual explanation of a term related to wiki article"
                         :max-results 3
                         :min-score   -0.6}
          http-response (perform-searching query-params)]
      (is (= 400 (:status http-response)))))

  (testing "correctness of documents searching in vector database, error response on wrong `min-score` param value"
    (let [query-params  {:text        "some textual explanation of a term related to wiki article"
                         :max-results 3
                         :min-score   1.6}
          http-response (perform-searching query-params)]
      (is (= 400 (:status http-response)))))

  (testing "correctness of documents searching in vector database"
    (with-redefs [embedding-store/search (spy/stub [(EmbeddingMatch. 0.61 "1" (Embedding/from [(float 0.1)]) (TextSegment/from "a" (Metadata/from "k1" "v1")))
                                                    (EmbeddingMatch. 0.62 "2" (Embedding/from [(float 0.2)]) (TextSegment/from "b" (Metadata/from "k2" "v2")))])]
      (let [query-params  {:text        "some textual explanation of a term related to wiki article"
                           :max-results 3
                           :min-score   0.6}
            http-response (perform-searching query-params)
            response-body (tu/parse-response-body http-response)]
        (is (= 200 (:status http-response)))

        (is (= true (spy/called? embedding-store/search)))

        (is (= [{:score 0.61 :metadata {:k1 "v1"} :text "a"}
                {:score 0.62 :metadata {:k2 "v2"} :text "b"}]
               response-body))))))

;;;

(defn obtain-metadata []
  (tu/make-http-get-call app-config/holder
                         "metadata"
                         {}))

(deftest metadata-obtaining
  (testing "correctness of ingested embeddings metadata obtaining"
    (let [http-response (obtain-metadata)
          response-body (tu/parse-response-body http-response)]
      (is (= 200 (:status http-response)))
      (is (= {:ingested-items-count 5} response-body)))))
