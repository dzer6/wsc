(ns wiki-stem-corpus.async.assertions
  (:require [clojure.test :as test]
            [clojure.tools.logging :as log])
  (:import (java.util.concurrent ExecutionException)))

(def ^:dynamic *eventually-polling-ms* 50)

(def ^:dynamic *eventually-timeout-ms* 10000)

(defn eventually* [expected actual]
  (let [^Long polling       *eventually-polling-ms*
        start-ts            (System/currentTimeMillis)
        deadline            (+ start-ts *eventually-timeout-ms*)
        time-until-deadline (fn [] (- deadline (System/currentTimeMillis)))]
    (loop [time-left (time-until-deadline)]
      (let [v (try
                (deref (actual)
                       time-left
                       ::timeout)
                (catch ExecutionException e
                  (.getCause e)))
            r (try
                (expected v)
                (catch Exception e
                  (log/error e)
                  nil))]
        (if r
          (do
            (log/infof "Time spent in loop for waiting the result: %dms" (- (System/currentTimeMillis) start-ts))
            [true v])
          (do (Thread/sleep polling)
              (let [time-left (time-until-deadline)]
                (if (pos? time-left)
                  (recur time-left)
                  [false v]))))))))

(declare eventually)

(defmethod test/assert-expr 'eventually [msg form]
  (let [expected-val (nth form 1)
        actual-forms (nthnext form 2)]
    `(let [result#     (eventually* (partial = ~expected-val)
                                    (fn [] (future ~@actual-forms)))
           success#    (first result#)
           actual-val# (second result#)]
       (if success#
         (test/do-report {:type     :pass
                          :message  ~msg
                          :expected ~expected-val
                          :actual   actual-val#})
         (test/do-report {:type     :fail
                          :message  ~msg
                          :expected ~expected-val
                          :actual   actual-val#})))))