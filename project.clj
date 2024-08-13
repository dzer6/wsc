(def camel-version "4.7.0")

(defproject com.dzer6/wiki-stem-corpus "1.0.0"
  :dependencies [[org.clojure/clojure "1.12.0-rc1"]
                 [org.clojure/core.memoize "1.1.266"]
                 [org.clojure/tools.logging "1.3.0"]

                 [com.dzer6/clj-camel "3.1.0"]
                 [org.apache.camel/camel-core ~camel-version]
                 [org.apache.camel/camel-management ~camel-version]
                 [org.apache.camel/camel-jcache ~camel-version]
                 [org.apache.camel/camel-file ~camel-version]
                 [org.apache.camel/camel-csv ~camel-version]
                 [org.apache.camel/camel-http ~camel-version]
                 [org.apache.camel/camel-sql ~camel-version]
                 [org.apache.camel/camel-zipfile ~camel-version]
                 [org.apache.camel/camel-opentelemetry ~camel-version]
                 [org.apache.camel/camel-endpointdsl ~camel-version]

                 [dev.langchain4j/langchain4j "0.33.0"]
                 [dev.langchain4j/langchain4j-ollama "0.33.0"]
                 [dev.langchain4j/langchain4j-pgvector "0.33.0"]

                 ;; Logging: use logback with slf4j, redirect JUL, JCL and Log4J:
                 [ch.qos.logback/logback-core "1.5.6"]
                 [ch.qos.logback/logback-classic "1.5.6"]
                 [ch.qos.logback.contrib/logback-json-classic "0.1.5"]
                 [ch.qos.logback.contrib/logback-jackson "0.1.5"]
                 [org.codehaus.janino/janino "3.1.12"]
                 [org.slf4j/slf4j-api "2.0.16"]
                 [org.slf4j/jul-to-slf4j "2.0.16"]          ; JUL to SLF4J
                 [org.slf4j/jcl-over-slf4j "2.0.16"]        ; JCL to SLF4J
                 [org.slf4j/log4j-over-slf4j "2.0.16"]      ; Log4j to SLF4J
                 [net.logstash.logback/logstash-logback-encoder "8.0"]

                 [mount "0.1.18"]

                 ;; Common:
                 [camel-snake-kebab "0.4.3"]
                 [prismatic/schema "1.4.1"]
                 [metosin/schema-tools "0.13.1"]
                 [clj-time "0.15.2"]
                 [clj-fuzzy "0.4.1"]
                 [slingshot "0.12.2"]

                 ;; REST API
                 [ring "1.12.2" :exclusions [ring/ring-jetty-adapter]]
                 [ring/ring-core "1.12.2"]
                 [ring/ring-defaults "0.5.0"]
                 [info.sunng/ring-jetty9-adapter "0.33.4"]
                 [metosin/compojure-api "2.0.0-alpha31" :exclusions [frankiesardo/linked
                                                                     metosin/ring-swagger-ui]]
                 [metosin/ring-swagger-ui "5.9.0"]

                 ;; Configuration:
                 [cprop "0.1.20"]

                 ;; Memoizing
                 [com.taoensso/encore "3.114.0"]

                 ;; JDBC
                 [com.zaxxer/HikariCP "5.1.0"]
                 [org.postgresql/postgresql "42.7.3"]
                 [com.github.seancorfield/next.jdbc "1.3.939"]
                 [com.layerware/hugsql-core "0.5.3"]
                 [com.layerware/hugsql-adapter-next-jdbc "0.5.3" :exclusions [seancorfield/next.jdbc]]
                 [org.flywaydb/flyway-core "10.17.0"]
                 [org.flywaydb/flyway-database-postgresql "10.17.0"]

                 ;; JSON encoding and decoding:
                 [metosin/jsonista "0.3.10"]
                 [com.fasterxml.jackson.core/jackson-core "2.17.2"]
                 [com.fasterxml.jackson.core/jackson-annotations "2.17.2"]]

  :main wiki-stem-corpus.main
  :source-paths ["src/main/clj"]
  :resource-paths ["src/main/resources"]
  :test-paths ["src/test/clj" "src/test/resources"]
  :uberjar-name "app.jar"

  :repl-options {:port 4010 :init-ns user}

  :plugins [[test2junit "1.4.2"]
            [lein-cljfmt "0.6.7"]
            [lein-ancient "0.6.15"]
            [jonase/eastwood "0.3.10" :exclusions [org.clojure/clojure]]
            [lein-codox "0.10.7"]
            [lein-kibit "0.1.8"]
            [lein-nvd "1.3.1"]
            [lein-cloverage "1.2.2"]
            [lein-bikeshed "0.5.2"]]

  :profiles {:user    {:source-paths          ["dev"
                                               "src/main/clj"]

                       :resource-paths        ["src/test/resources"
                                               "src/main/resources"]

                       :test2junit-output-dir "./target/test-reports/"

                       :dependencies          [[com.cemerick/pomegranate "1.1.0"]
                                               [org.clojure/tools.nrepl "0.2.13"]

                                               [clj-http "3.13.0"]

                                               [org.testcontainers/testcontainers "1.20.1"]
                                               [org.testcontainers/postgresql "1.20.1"]

                                               [tortue/spy "2.15.0"]]}
             :uberjar {:aot         :all
                       :omit-source true}})