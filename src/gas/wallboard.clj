(ns gas.wallboard
  (:require
   [bidi.bidi :as bidi]
   [clojure.tools.logging :refer :all]
   [clojure.string :as str]
   [selmer.parser :as selmer]
   [schema.core :as s]
   [yada.yada :as yada]
   [yada.swagger :as swagger]
   [clojure.core.async :as async]
   [manifold.deferred :as d]
   [manifold.stream :as stream]
   [aleph.http :as http]
   [byte-streams :as bs]
   [gas.db :as db]))

(defn slug->title [slug]
  (str/replace slug "_" " "))

(defn next-rev-str [slug]
  (str slug "_" (.getTime (java.util.Date.)) "_" (java.util.UUID/randomUUID)))

(defn with-article-links [ctx attr-map]
  (assoc attr-map
         :article/title (slug->title (:article/slug attr-map))
         :wiki/links
         {:article/show (yada/url-for ctx ::article
                                      {:route-params
                                       {:slug (:article/slug attr-map)}})
          :article/edit (yada/url-for ctx ::article-edit
                                      {:route-params
                                       {:slug (:article/slug attr-map)}})
          :article/history (yada/url-for ctx ::article-history
                                      {:route-params
                                       {:slug (:article/slug attr-map)}})}))

(defn async->manifold [c]
  ;; core async into manifold
  (let [d (d/deferred)]
    (async/take! c
                 (fn [item]
                   (d/success! d item)))
    d))



(defn md-escape-html [text state]
  ;; https://github.com/yogthos/markdown-clj/issues/36
  [(clojure.string/escape text
                           {\& "&amp;"
                            \< "&lt;"
                            \> "&gt;"
                            \" "&quot;"
                            \' "&#39;"})
   state])


(defn- entry-map->vector [ctx m]
  (sort-by
   :id
   (reduce-kv
    (fn [acc k v]
      (conj acc
            (assoc v
                   :id k
                   :href (yada/href-for
                          ctx
                          :gas.resources/wallboard-entry
                          {:route-params {:id k}}))))
    [] m)))

(defn new-index-resource [db]
  (yada/resource
   {:id :gas.resources/wallboard-index
    :description "Available Wallboards"
    :produces [{:media-type
                #{"text/html" "application/edn;q=0.9" "application/json;q=0.8" "application/transit+json;q=0.7"}
                :charset "UTF-8"}]
    :methods
    {:get {:parameters {:query {(s/optional-key :q) String}}
           :swagger/tags ["default" "getters"]
           :response (fn [ctx]
                       (let [q (get-in ctx [:parameters :query :q])
                             entries (if q
                                       (db/search-entries db q)
                                       (db/get-entries db))]
                         (case (yada/content-type ctx)
                           "text/html" (selmer/render-file
                                        "phonebook.html"
                                        {:title "Atea Wallboards"
                                         :ctx ctx
                                         :entries (entry-map->vector ctx entries)
                                         :q q})
                           entries)))}

     :post {:parameters {:form {:surname String :firstname String :phone String}}
            :consumes #{"application/x-www-form-urlencoded"}
            :response (fn [ctx]
                        (let [id (db/add-entry db (get-in ctx [:parameters :form]))]
                          (java.net.URI. (:uri (yada/uri-info ctx :edge.resources/phonebook-entry {:route-params {:id id}})))))}}}))


(defn wallboard-routes [db {:keys [port]}]
  (let [routes ["/wb"
                [
                 ;; Phonebook index
                 ["" (new-index-resource db)]
                 ;; Phonebook entry, with path parameter
                 [["/" :id] (new-index-resource db)]]]]
    [""
     [
      routes

      ;; Swagger
      ["/wallboard-api/swagger.json"
       (bidi/tag
        (yada/handler
         (swagger/swagger-spec-resource
          (swagger/swagger-spec
           routes
           {:info {:title "Wallboard"
                   :version "1.0"
                   :description "A simple application that demonstrates the use of multiple HTTP methods"}
            :host (format "localhost:%d" port)
            :schemes ["http"]
            :tags [{:name "getters"
                    :description "All paths that support GET"}]
            :basePath ""})))
        :gas.resources/wallboard-swagger)]]]))
