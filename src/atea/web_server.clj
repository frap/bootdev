;; Copyright © 2016, JUXT LTD.

(ns atea.web-server
  (:require
   [bidi.bidi :refer [tag]]
   [bidi.vhosts :refer [make-handler vhosts-model]]
   [clojure.tools.logging :refer :all]
   [com.stuartsierra.component :refer [Lifecycle using]]
   [clojure.java.io :as io]
   [hiccup.core :refer [html]]
   [schema.core :as s]
   [selmer.parser :as selmer]
   [yada.resources.webjar-resource :refer [new-webjar-resource]]
   [yada.yada :refer [handler resource] :as yada]
   )
  )

(defn content-routes []
  ["/"
   [
    ["index.html"
     (yada/resource
      {:id :atea.resources/index
       :methods
       {:get
        {:produces #{"text/html"}
         :response (fn [ctx]
                     (selmer/render-file "index.html" {:title "Atea Index"
                                                       :ctx ctx}))}}})]

    ["" (assoc (yada/redirect :atea.resources/index) :id :atea.resources/content)]

    ;; Add some pairs (as vectors) here. First item is the path, second is the handler.
    ;; Here's an example

    [""
     (-> (yada/as-resource (io/file "target"))
         (assoc :id :atea.resources/static))]]])

(defn routes
  "Create the URI route structure for atea application."
  [db config]
  [""
   [
    ;;peep
    ["/peep" (yada/handler "Hola Peeps!\n")]

    ["/css/atea.css"
     (-> (yada/as-resource (io/resource "public/css/atea.css"))
         (assoc :id ::stylesheet))]

    ;; Our content routes, and potentially other routes.
    (content-routes)

    ;; This is a backstop. Always produce a 404 if we ge there. This
    ;; ensures we never pass nil back to Aleph.
    [true (yada/handler nil)]]])

(s/defrecord WebServer [host :- s/Str
                        port :- s/Str
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
