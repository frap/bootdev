;; Copyright © 2017, Red Elvis.
(ns atea.db
  (:require   [com.stuartsierra.component :refer [Lifecycle]]
              [duct.component.hikaricp :refer [hikaricp]]
              [clojure.spec.alpha :as s]
              [clojure.future :refer :all]

              )
)


(def jdbc-regex #"jdbc:[a-zA-Z0-9._+-]+:[a-zA-Z0-9._/;=:]+")

(s/def ::jdbc-type     (s/and string? #(re-matches jdbc-regex %)))
(s/def ::stringornil   (s/nilable string?))
(s/def ::jdbc-timeout  (s/nilable (s/and pos-int? #(> % 250))))
(s/def ::posintornil   (s/nilable pos-int?))

(defn create-db  [entries]
  (assert entries)
  {:db (atom entries)})

(defn new-hrdb [config]
  (hikaricp    {:pool-name "UccxHR"
                :uri  (:hruri  config)
                :username "uccxhruser"
                :password (:hrpasswd config)})
  )



(defn search-entries [db q]
  (println db q))

(defn get-entries [db]
  println db)

(defn add-entry [db]
  println db)


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
