(ns wiki-stem-corpus.rpc.api
  (:require [base.api.error :as error]
            [wiki-stem-corpus.rpc.controller.langchain :as langchain]
            [wiki-stem-corpus.rpc.controller.health :as health]
            [compojure.api.sweet :as sw]
            [compojure.route :as route]
            [ring.util.http-response :as ring.http]
            [schema.core :as s]))

(s/defschema ErrorResponse
  {:details     s/Any
   :error       s/Str
   :description s/Str})

(defn routes []
  (sw/routes
    (sw/api
      {:exceptions
       {:handlers
        {:compojure.api.exception/request-validation  error/request-validation-handler
         :compojure.api.exception/response-validation error/response-validation-handler
         :compojure.api.exception/default             error/default-handler}}
       :swagger
       {:ui   "/api-docs"
        :spec "/swagger.json"
        :data {:info     {:version     "1.0.0"
                          :title       "Wiki STEM Corpus API"
                          :description "REST API for searching a term by description"}
               :consumes ["application/json"]
               :produces ["application/json"]}}}

      (sw/GET "/search" []
        :query-params [text :- String
                       max-results :- Number
                       min-score :- Double]
        :return langchain/SearchResponse
        :summary "Returns a list of terms with corresponding descriptions"
        :responses {400 {:schema ErrorResponse :description "Wrong input data"}
                    500 {:schema ErrorResponse :description "Internal issue"}}
        (ring.http/ok (langchain/search text max-results min-score)))

      (sw/GET "/metadata" []
        :return langchain/WikiStemCorpusMetadataResponse
        :summary "Returns metadata for the ingested Wiki STEM Corpus"
        :responses {400 {:schema ErrorResponse :description "Wrong input data"}
                    500 {:schema ErrorResponse :description "Internal issue"}}
        (ring.http/ok (langchain/metadata)))

      (sw/GET "/health/live" []
        :summary "Check liveness probe"
        :return health/HealthCheckResponse
        (health/liveness-handler))

      (sw/GET "/health/ready" []
        :summary "Check readiness probe"
        :return health/HealthCheckResponse
        (health/readiness-handler))

      (sw/GET "/" []
        :summary "Redirect to Swagger UI"
        (ring.http/moved-permanently "/api-docs"))

      (sw/undocumented
        (route/not-found "Not Found")))))
