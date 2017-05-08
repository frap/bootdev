;; A complete development environment for websites in Clojure and
;; ClojureScript.

;; Most users will use 'boot dev' from the command-line or via an IDE
;; (e.g. CIDER).

;; See README.md for more details.

(require '[clojure.java.shell :as sh])

(defn next-version [version]
  (when version
    (let [[a b] (next (re-matches #"(.*?)([\d]+)" version))]
      (when (and a b)
        (str a (inc (Long/parseLong b))))))
  )

(defn deduce-version-from-git
  "Avoid another decade of pointless, unnecessary and error-prone
  fiddling with version labels in source code."
  []
  (let [[version commits hash dirty?]
        (next (re-matches #"(.*?)-(.*?)-(.*?)(-dirty)?\n"
                          (:out (sh/sh "git" "describe" "--dirty" "--long" "--tags" "--match" "[0-9].*"))))]
    (cond
      dirty? (str (next-version version) "-" hash "-dirty")
      (pos? (Long/parseLong commits)) (str (next-version version) "-" hash)
      :otherwise version)))

(def project "gas-sample")
(def version (deduce-version-from-git))

(set-env!
 :source-paths    #{"src"}
 :resource-paths  #{"resources"
                    "src" ;; add sources to uberjar
                    }
 :repositories #(conj % '["ibm" {:url "https://mvnrepository.com/artifact/com.ibm.informix/jdbc"}])
 :dependencies
 '[
   [boot-deps "1.6" :scope "test"] ;;  ancient for boot 
   [adzerk/boot-cljs "1.7.228-1" :scope "test"]
   [adzerk/boot-cljs-repl "0.3.3" :scope "test"]
   [adzerk/boot-reload "0.5.1" :scope "test"]
   [adzerk/bootlaces "0.1.13" scope "test"]
   [weasel "0.7.0" :scope "test"] ;; Websocket Server
   ;;[deraen/boot-sass "0.3.0" :scope "test"]
   [reloaded.repl "0.2.3" :scope "test"]
   ;; Needed for start-repl in cljs repl
   [com.cemerick/piggieback "0.2.1" :scope "test"]
   [org.clojure/clojure "1.8.0"]
   [org.clojure/core.incubator "0.1.4"]
   [org.clojure/clojurescript "1.9.229"]
   [org.clojure/tools.nrepl "0.2.12"]

   ;; Server deps
   [aero "1.1.2"]
   [bidi "2.0.16"]
   [aleph "0.4.3"]
   [com.stuartsierra/component "0.3.2"]
   [org.clojure/tools.namespace "0.2.11"]
   ;;[hiccup "1.0.5"]
   ;;[prismatic/schema "1.1.3"]
   ;;[selmer "1.10.6"]
   ;;[yada "1.2.1" :exclusions [aleph manifold ring-swagger prismatic/schema]]
   ;;[metosin/ring-swagger "0.22.10"]
   [clj-time "0.13.0"]

   ;; DB dependencies
   ;;[com.layerware/hugsql "0.4.7"]
   [org.clojure/java.jdbc "0.7.0-alpha1"]
   [atea/hikaricp-component "0.1.6"]
   [com.ibm.informix/jdbc "4.10.8.1"]
   ;;[local/ojdbc6 "11.2.0.4"]
   ;;[datascript "0.15.5"]

   ;;logging
   [org.clojure/tools.logging "0.3.1"]
   [adzerk/boot-logservice "1.2.0"]

   ;; App deps
   ;;[reagent "0.6.0"]

   ;; Exceptions
   [dire "0.5.4"]
   [slingshot "0.12.2"]
   ]
 )

(require '[com.stuartsierra.component :as component]
         'clojure.tools.namespace.repl
         '[gas.system :refer [new-system]]
         )

(def repl-port 5600)

(task-options!
 pom {:project (symbol project)
      :version version
      :description "A sample boot project setup"
      :license {"The MIT License (MIT)" "http://opensource.org/licenses/mit-license.php"}}
)



(deftask dev-system
  "Develop the server backend. The system is automatically started in
  the dev profile."
  []
  (require 'reloaded.repl)
  (let [go (resolve 'reloaded.repl/go)]
    (try
      (require 'user)
      (go)
      (catch Exception e
        (boot.util/fail "Exception while starting the system\n")
        (boot.util/print-ex e))))
  identity)

(deftask dev
  "This is the main development entry point."
  []
  (set-env! :dependencies #(vec (concat % '[[reloaded.repl "0.2.3"]])))
  (set-env! :source-paths #(conj % "dev"))

  ;; Needed by tools.namespace to know where the source files are
  (apply clojure.tools.namespace.repl/set-refresh-dirs (get-env :directories))

  (comp
   (watch)
   (cider)
   (repl :server true
         :port repl-port
         :init-ns 'user)
   (dev-system)
   (target)))

(deftask build
  []
  (target :dir #{"static"}))

(defn- run-system [profile]
  (println "Running system with profile" profile)
  (let [system (new-system profile)]
    (component/start system)
    (intern 'user 'system system)
    (with-pre-wrap fileset
      (assoc fileset :system system))))

(deftask run [p profile VAL kw "Profile"]
  (comp
   (repl :server true
         :port (case profile :prod 5601 :beta 5602 5600)
         :init-ns 'user)
   (run-system (or profile :prod))
   (wait)))

(deftask uberjar
  "Build an uberjar"
  []
  (println "Building Gas uberjar")
  (comp
   (aot)
   (pom)
   (uber)
   (jar)
   (target)))

(deftask show-version "Show version" [] (println version))
