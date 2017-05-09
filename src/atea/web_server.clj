;; Copyright © 2016, JUXT LTD.

(ns atea.web-server
  (:require
   [bidi.bidi :refer [tag]]
   [bidi.vhosts :refer [make-handler vhosts-model]]
   [clojure.tools.logging :refer :all]
   [com.stuartsierra.component :refer [Lifecycle using]]
   [clojure.java.io :as io]
   ;;[schema.core :as s]
  ;; [selmer.parser :as selmer]
   [yada.resources.webjar-resource :refer [new-webjar-resource]]
   [yada.yada :refer [resource] :as yada]
   [yada.handler :as handler]
   [clojure.walk :as walk]
   )
  )

(defn routes
  "Create the URI route structure for our application."
  [config]
  [""
   [["/" (yada/redirect ::index)]

    ["/sample.css"
     (-> (yada/as-resource (io/resource "public/css/sample.css"))
         (assoc :id ::stylesheet))]

    ;; This is a backstop. Always produce a 404 if we ge there. This
    ;; ensures we never pass nil back to Aleph.
    [true (yada/handler nil)]]])

(defrecord WebServer [host
                      port
                      db
                      listener]
  Lifecycle
  (start [component]
    (if listener
      component                         ; idempotence
      (let [vhosts-model
            (vhosts-model
             [{:scheme :http :host host}
              (routes db {:port port})])
            listener (yada/listener vhosts-model {:port port})]
        (infof "Started web-server on port %s" (:port listener))
        (assoc component :listener listener))))

  (stop [component]
    (when-let [close (get-in component [:listener :close])]
      (close))
    (assoc component :listener nil)))

(defn new-web-server []
  (using
   (map->WebServer {})
   [:db ]))
