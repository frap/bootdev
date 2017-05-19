(ns products.web-test
(:require [clojure.test :refer :all]
[clj-http.client :as client]
[products.web :refer :all]
[com.stuartsierra.component :as component]))

(def server-test-port 3339)
(def server-test-host "localhost")

(defn test-system
[config-options]
(component/system-map
:web-server (make-web-server (:port config-options))))

(defn with-system
[f]
(let [the-system (component/start (test-system {:port server-test-port}))]
(f)
(component/stop the-system)))

(use-fixtures :once with-system)

(deftest ^:integration status-endpoint-is-accessible
(let [response (client/get
(str "http://" server-test-host ":" server-test-port "/status")
{:throw-exceptions false})]
(is (= 200 (:status response)))))

(deftest ^:integration get-404-for-unknown-route
(let [response (client/get
(str "http://" server-test-host ":" server-test-port "/unknown")
{:throw-exceptions false})]
(is (= 404 (:status response)))))
