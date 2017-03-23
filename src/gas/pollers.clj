(ns gas.pollers
  (:require    [manifold.stream :as s]
               [manifold.deferred :as d]
               [manifold.time :as t]
               [aleph.http :as http]
               [byte-streams :as bs]
               [gas.time :refer [yesterday]]
               [gas.sql  :as sql]
               [datascript.core :as ds]
               )
  )

;;(defonce uccx-stats (atom {}))

(def schema {:skill {:db/cardinality :db.cardinality/many}})


(defn strm-consume [f stream]
  (d/loop []
    (d/chain (s/take! stream ::drained)

      ;; if we received a message, run it through `f`
      (fn [msg]
        (if (identical? ::drained msg)
          ::drained
          (f msg)))

      ;; wait for the result from `f` to be realised, and
      ;; recur, unless the stream is already drained
      (fn [result]
        (when-not (identical? ::drained result)
          (d/recur)))
      )
     ;; catch exceptions
    ;;(d/catch Exception #(println "Not a valid UCCX URL!: " % ))
    ))

(defn check-master [url]
  (let [d (http/get url)]
    (-> d (d/chain :body
                   bs/to-string
                   )
        (d/catch (fn [ex]  (println "Not a valid UCCX URL!: " ex )))
        ))
  )

(defn getrts [ uccxdb ]
  (into {} (sql/realtimestats uccxdb)))

(defn getgos    [ uccxdb ]
  (into {} (sql/gos uccxdb {:starttime yesterday})))

(defn poll   [ uccxdb func ]
   (into {}  ( func uccxdb)))

;;(pprint (sql/callsdirectin hrspec {:viewtime yesterday}))

;;(pprint (sql/agent-queues hrspec))

;;(sql/test-sqlvec)

;;(pprint (sql/abandoned hrspec));

;;(j/query hrspec  ["{call sp_abandoned_calls_activity(?,?,'0',null) }"] :as-arrays? true)


(defn rts-period
   "Polls for real time Stats RTICDStatistics from remote UCCX database
   every poll-interval (secs) and merges with local atom"
  [ uccxdb stats-db poll-interval ]
  (strm-consume
   #(swap! stats-db merge {:rts %})
   (s/periodically (* 1000 poll-interval) #(getrts uccxdb)))
  )

(defn gos-period
  "Polls for Grade of Service Statistics from remote UCCX database
   every poll-interval (secs) and merges with local atom"
  [ uccxdb stats-db poll-interval ]
  (strm-consume
   #(swap! stats-db merge {:gos %})
   (s/periodically (* 1000 poll-interval) #(getgos uccxdb)))
  )

;;(s/consume println rts-period)
;;(strm-consume pprint (rts-period ))
;;(s/description rts-period)

;;(s/close! rts-period)



;; @(t/in 1000 getrts)


(defn slow-echo-handler
  [f]
  (fn [stream info]
    (d/loop []

      ;; take a message, and define a default value that tells us if the connection is closed
      (-> (s/take! stream ::none)

        (d/chain

          ;; first, check if there even was a message, and then transform it on another thread
          (fn [msg]
            (if (= ::none msg)
              ::none
              (d/future (f msg))))

          ;; once the transformation is complete, write it back to the client
          (fn [msg']
            (when-not (= ::none msg')
              (s/put! stream getrts)))

          ;; if we were successful in our response, recur and repeat
          (fn [result]
            (when result
              (d/recur)))
        )

        ;; if there were any issues on the far end, send a stringified exception back
        ;; and close the connection
        (d/catch
          (fn [ex]
            (s/put! stream (str "ERROR: " ex))
            (s/close! stream)))
        ))))
