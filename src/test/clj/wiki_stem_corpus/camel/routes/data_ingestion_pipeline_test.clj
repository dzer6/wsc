(ns wiki-stem-corpus.camel.routes.data-ingestion-pipeline-test
  (:require [clojure.test :refer :all]
            [spy.core :as spy]
            [wiki-stem-corpus.async.assertions :refer :all]
            [wiki-stem-corpus.camel.routes.data-ingestion-pipeline :refer :all]
            [wiki-stem-corpus.camel.routes.dead-letter]
            [wiki-stem-corpus.config :as app-config]
            [wiki-stem-corpus.fixtures :as fixtures]
            [wiki-stem-corpus.langchain.embedding-store :as embedding-store]))

(->> [fixtures/with-test-config
      (fixtures/with-postgres app-config/holder)
      fixtures/with-postgres-migrations
      fixtures/with-camel-context
      (fixtures/with-file-for-ingestion app-config/holder "src/test/resources/data/wiki_stem_corpus_sample.zip")
      (fixtures/with-camel-routes #'wiki-stem-corpus.camel.routes.data-ingestion-pipeline/*routes*
                                  #'wiki-stem-corpus.camel.routes.dead-letter/*routes*)]
     (join-fixtures)
     (use-fixtures :once))

(def page-title-1 "Introduction to Nanomaterials")
(def page-title-2 "Two dimensional (2D) nanomaterials")
(def page-title-3 "Properties of Graphene")
(def page-title-4 "Quantum Dots and Their Applications")
(def page-title-5 "Overview of Nanotechnology")

(def text-1 "Nanomaterials are materials with structural features smaller than 100 nanometers.")
(def text-2 "Two-dimensional nanomaterials have unique properties useful in various applications.")
(def text-3 "Graphene is a single layer of carbon atoms arranged in a hexagonal lattice.")
(def text-4 "Quantum dots are semiconductor nanocrystals with quantum mechanical properties.")
(def text-5 "Nanotechnology involves manipulation of matter on an atomic, molecular, and supramolecular scale.")

(deftest data-ingestion-happy-path
  (testing "correctness of zipped CSV file ingestion"
    (binding [*eventually-polling-ms* 700
              *eventually-timeout-ms* 5000]
      (with-redefs [embedding-store/add (spy/stub)]
        (is (eventually true
                        (spy/called-n-times? embedding-store/add 5)))

        (is (eventually true
                        (spy/called-with? embedding-store/add "page_title" page-title-1 text-1)))

        (is (eventually true
                        (spy/called-with? embedding-store/add "page_title" page-title-2 text-2)))

        (is (eventually true
                        (spy/called-with? embedding-store/add "page_title" page-title-3 text-3)))

        (is (eventually true
                        (spy/called-with? embedding-store/add "page_title" page-title-4 text-4)))

        (is (eventually true
                        (spy/called-with? embedding-store/add "page_title" page-title-5 text-5)))))))

