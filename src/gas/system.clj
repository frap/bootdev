(ns gas.system
  (require '[com.stuartsierra.component :as component]))

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
  component/Lifecycle

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
