;; Copyright © 2016, JUXT LTD.

(ns user
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.test :refer [run-all-tests]]
   [clojure.reflect :refer [reflect]]
   [clojure.repl :refer [apropos dir doc find-doc pst source]]
   [clojure.tools.namespace.repl :refer [refresh refresh-all]]
   [clojure.java.io :as io]
   [com.stuartsierra.component :as component]
   [reloaded.repl :refer [system init start stop go reset reset-all]]
   ;; [clojure.core.async :as a :refer [>! <! >!! <!! chan buffer dropping-buffer sliding-buffer close! timeout alts! alts!! go-loop]]
   [gas.system :as system]
   [gas.sql :as sql]
   [gas.pollers :as poll]
   [clojure.java.jdbc :as j]
   [manifold.stream :as s]
   [manifold.deferred :as d]
   [manifold.time :as t]
   [aleph.http :as http]
   ;; time
   ;;[schema.core :as s]
   ;;[yada.test :refer [response-for]]
   ;;logging
  ;; [adzerk/boot-logservice :as log-service]
  ;; [clojure.tools.logging  :as log]
   ;; Exceptions
   [dire.core :refer [with-handler! with-finally!]]

   ))


(defn new-dev-system
  "Create a development system"
  []
  (let [config (system/config :dev)]
    (system/configure-components
     (component/system-using
      (system/new-system-map config)
      (system/new-dependency-map))
     config)))

;;(Alter-var-root #'log/*logger-factory* (constantly (log-service/make-factory log-config)))

(reloaded.repl/set-init! new-dev-system)

;;(def wallspec  (:spec (:wall-db system)))

;;(def hrspec    (:spec (:hr-db system)))

(def urlgood "http://9.1.1.103/uccx/isDBMaster")
(def urlbad  "http://9.1.1.120/uccx/isDBMaster")

;;(pprint (poll/check-master urlbad))

(defn get-latest-stats [] (-> pprint (:uccx-stats system)))
;;(pprint (poll/getgos hrspec))

;;(poll/strm-consume pprint (poll/gos-period hrspec))
;;(s/close poll/gos-period)

(defn test-all []
  (run-all-tests #"gas.*test$"))

(defn reset-and-test []
  (reset)
  (time (test-all)))

(defn cljs-repl
  "Start a ClojureScript REPL"
  []
  (eval
   '(do (in-ns 'boot.user)
        (start-repl))))

;; REPL Convenience helpers
