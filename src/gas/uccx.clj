(ns gas.uccx
  (:require  [clojure.pprint :refer [pprint]]
             [clojure.java.jdbc  :as j]
             [com.stuartsierra.component :as component]
             [duct.component.hikaricp :refer [hikaricp]]
             [aleph.http :as http]
             [byte-streams :as bs]
             [manifold.stream :as s]
             [manifold.deferred :as d]
             [manifold.time :as t]
             [dire.core :refer [with-handler! with-finally!]]
   )
  )

(defn check-master [url]
  (d/chain (http/get url)
           :body
           bs/to-string
           )
  )

(defn new-uccx-system
  "Create a UCCX poll system"
  [ server hostip passwd]
  (let [pool   {:uri (str "jdbc:informix-sqli://" hostip ":1504/db_cra:informixserver=" server "_uccx")
                :username "uccxwallboard"
                :password passwd}
        uccx   (component/start (hikaricp pool))
        spec   (:spec uccx)
        master (str "http://" hostip "/uccx/isDBMaster")
        ]

  (if (re-find #"isMaster>true" @(check-master master) )
       (do (println server "is the current master\n")
           (pprint  (into {} (j/query spec
                                  ["SELECT * FROM RtICDStatistics"])))
          )
       (println server "is NOT the current master!\n")
       )
  ;;(component/stop uccx)
    )
  )
