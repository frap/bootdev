;; Copyright © 2017, Red Elvis.
(ns gas.db
  (:require   [com.stuartsierra.component :refer [Lifecycle]]
              [duct.component.hikaricp :refer [hikaricp]]
              )
)

(defn new-hrdb [config]
  (hikaricp    {:pool-name "UccxHR"
                :uri  (:hruri  config)
                :username "uccxhruser"
                :password (:hrpasswd config)})
  )

(defn new-walldb [config]
  (hikaricp      {:pool-name "UccxWallbd"
                  :uri (:walluri config)
                  :username "uccxwallboard"
                  :password (:wallpasswd config)})
  )



(def wallb-dbspec { :classname "com.informix.jdbc.IfxDriver"
                   :subprotocol "informix-sqli"
                   :subname "//9.1.1.62:1504/db_cra:informixserver=atea_dev_uccx11_uccx"
                  :user "uccxwallboard"
                  :password "ateasystems0916"})


(defn db-spec [subprotocol driver-classname subname user password]
  {:subprotocol    subprotocol
   :classname      driver-classname
   :subname        subname
   :user           user
   :password       password
   :make-pool?     true
   :naming         {:keys   clojure.string/lower-case
                    :fields clojure.string/upper-case}})

(defrecord Database [subprotocol driver-classname subname user password]
  Lifecycle

  (start [component]
    (if-let [dbspec (:connection component)]
      (do
        (println ";; db connection already established")
        component)
      (let [dbspec (db-spec subprotocol
                            driver-classname
                            subname
                            user
                            password)]
        (println (str ";; connected to " user "@" subprotocol ":"
                      subname " ..."))
        ;;(ddl/recreate-tables-safe! dbspec)
        (assoc component :connection dbspec))))

  (stop [component]
    (if-let [dbspec (:connection component)]
      (println ";; tearing down database connection")
      (println ";; no db connection exists"))
    (assoc component :connection nil)))

(defn new-db [m]
  (map->Database m))
