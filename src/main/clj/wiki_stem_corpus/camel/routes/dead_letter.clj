(ns wiki-stem-corpus.camel.routes.dead-letter
  (:require [clj-camel.core :as c]
            [clj-camel.util :as cu]
            [clojure.tools.logging :as log]
            [mount.core :as mount]
            [wiki-stem-corpus.camel.state :as state]))

(def dead-letter-route-id "dead-letter-route")

(defn dead-letter-route []
  (c/route-builder
    (c/from "direct:dead-letter")
    (c/route-id dead-letter-route-id)
    (c/process cu/kebabify-keys)
    (c/process cu/put-message-body-to-map)
    (c/process (cu/merge-from-header-to-body :error-cause))
    (c/debug-exchange)
    (c/log ">>> ERROR ${body}")))

;;;

(defonce started? (atom false))

(defn start! []
  (when (not @started?)
    (log/infof "Adding `%s` route to Camel context" dead-letter-route-id)
    (c/add-routes @state/context (dead-letter-route))
    (reset! started? true)))

(defn stop! []
  (let [initialized?  @started?
        camel-context @state/context]
    (when (and initialized? (some? camel-context))
      (log/infof "Removing `%s` route from Camel context" dead-letter-route-id)
      (c/stop-and-remove-route camel-context dead-letter-route-id)
      (reset! started? false))))

(declare ^:dynamic *routes*)

(mount/defstate ^:dynamic *routes*
  :start (start!)
  :stop (stop!))
