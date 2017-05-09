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
   [manifold.stream :as s]
   [manifold.deferred :as d]
   [manifold.time :as t]
   [aleph.http :as http]
   ;; time
   ;;[schema.core :as s]
   ;;[yada.test :refer [response-for]]
   ;;logging
   ;;[adzerk/boot-logservice :as log-service]
   ;;[clojure.tools.logging  :as log]
   ;; Exceptions
   [dire.core :refer [with-handler! with-finally!]]

   ))

(def log-config
  [:configuration {:scan true, :scanPeriod "10 seconds"}
   [:appender {:name "FILE" :class "ch.qos.logback.core.rolling.RollingFileAppender"}
    [:encoder [:pattern "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"]]
    [:rollingPolicy {:class "ch.qos.logback.core.rolling.TimeBasedRollingPolicy"}
     [:fileNamePattern "logs/%d{yyyy-MM-dd}.%i.log"]
     [:timeBasedFileNamingAndTriggeringPolicy {:class "ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP"}
      [:maxFileSize "64 MB"]]]
    [:prudent true]]
   [:appender {:name "STDOUT" :class "ch.qos.logback.core.ConsoleAppender"}
    [:encoder [:pattern "%-5level %logger{36} - %msg%n"]]
    [:filter {:class "ch.qos.logback.classic.filter.ThresholdFilter"}
     [:level "INFO"]]]
   [:root {:level "INFO"}
    [:appender-ref {:ref "FILE"}]
    [:appender-ref {:ref "STDOUT"}]]
   [:logger {:name "user" :level "ALL"}]
   [:logger {:name "boot.user" :level "ALL"}]])

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
