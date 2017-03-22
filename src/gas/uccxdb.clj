(ns gas.uccxdb
  (:require  [clojure.tools.logging :as log]
             [clojure.pprint :refer [pprint]]
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

(defn check-master-uccx
  "Checks if the IP given is the UCCX DB master.
   returns boolean"
  [ hostip ]
  (let [url (str "http://" hostip  "/uccx/isDBMaster") ]
    (d/chain (http/get url)
           :body
           bs/to-string
           )
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

        ]

  (if (re-find #"isMaster>true" @(check-master master) )
       (do (println server "is the current master\n")
           (pprint  (into {} ))
          )
       (println server "is NOT the current master!\n")
       )
  ;;(component/stop uccx)
    )
  )

(defn make-conn [args-map]
  (let [hostip (:hostip args-map)
        master-uccx (check-master-uccx hostip)
        conn
        s    (s/stream)]
    (if (re-find #"isMaster>false" @master-uccx )
      (throw (ex-info "UCCX DB server is NOT master"
                      (merge conn
                             args-map)))
      (let [db (client/db conn)
            _ (log/info "poking UCCX master connection")
            poke (j/query conn
                                  ["SELECT * FROM RtICDStatistics"])]
        (log/info (str "poked UCCX master DB connection: " poke))
        (if (client/error? poke)
          ;; assume the peer-server has been restarted, can't keep connection to restarted peer-server running 'mem' database
          (if (= (:attempts-left args-map) 0)
            (throw (ex-info "Exhausted connection attempts to datomic peer server, is it running?" args-map))
            (make-conn (-> args-map
                           (assoc :cache-bust (java.util.UUID/randomUUID))
                           (update :attempts-left (fnil dec 5)))))
          conn
          )))))


(defrecord UccxPoll [args-map]
  component/Lifecycle
  (start [component]
    (let [args-map (assoc args-map
                          :account-id client/PRO_ACCOUNT)
          _ (log/info "First connection to UCCX DB")]
      (assoc component :get-conn (fn []
                                   (make-conn args-map)))))

  (stop [component]
    (assoc component :get-conn nil)))


(defn component []
  (map->UccxPoll {}))
