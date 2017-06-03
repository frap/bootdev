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
   [atea.system :as system]
   ;;[atea.sql :as sql]

   [clojure.java.jdbc :as j]
   [manifold.stream :as strm]
   [manifold.deferred :as d]
   [manifold.time :as t]
   [aleph.http :as http]
   ;; time
   [schema.core :as s]
   [yada.test :refer [response-for]]
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

(reloaded.repl/set-init! new-dev-system)

(defn check
  "Check for component validation errors"
  []
  (let [errors
        (->> system
             (reduce-kv
              (fn [acc k v]
                (assoc acc k (s/check (type v) v)))
              {})
             (filter (comp some? second)))]

    (when (seq errors) (into {} errors))))

(defn get-latest-stats [] (-> pprint (:db system)))


(defn test-all []
  (run-all-tests #"atea.*test$"))

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
