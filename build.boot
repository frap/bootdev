;; Copied off of JUXT edge all rights to JUXT LTD
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
      :otherwise (str version))))

(def project "gas-sample")
(def version (deduce-version-from-git)
  )

(set-env!
 :source-paths    #{"src"}
 :test-paths #{"test"}
 :resource-paths  #{"resources"
                    "src" ;; add sources to uberjar
                    }
 :asset-paths #{"assets"}
 :repositories #(conj % '["ibm" {:url "https://mvnrepository.com/artifact/com.ibm.informix/jdbc"}])
 :dependencies
 '[  [org.clojure/clojure "1.8.0" :scope "provided"]         ;; add CLJ
     [clojure-future-spec "1.9.0-alpha16-1"]
     ;;[org.clojure/core.incubator "0.1.4"]
     [org.clojure/clojurescript "1.9.521"] ;; add CLJS

     [adzerk/boot-cljs "2.0.0"      :scope "test"]
     [pandeiro/boot-http "0.8.0"        :scope "test"]
     [adzerk/boot-reload "0.5.1"        :scope "test"]
     [adzerk/boot-cljs-repl "0.3.3"     :scope "test"]    ;; add bREPL
     [com.cemerick/piggieback "0.2.1"   :scope "test"]    ;; needed by bREPL
     [weasel "0.7.0"                    :scope "test"]    ;; websocket srv
     [org.clojure/tools.nrepl "0.2.13"  :scope "test"]    ;; needed by bREPL
     [reloaded.repl "0.2.3"             :scope "test"]
     [boot-deps "0.1.6"                 :scope "test"]    ;;  ancient for boot

     ;;[deraen/boot-sass "0.3.1" :scope "test"]
     ;;TESTing
     [adzerk/boot-test "1.2.0"          :scope "test"]
     [crisptrutski/boot-cljs-test "0.3.1-SNAPSHOT" :scope "test"]


     ;; Server deps
     [aero "1.1.2"]
     [bidi "2.1.1"]
     [aleph "0.4.4-alpha3"]
     [com.stuartsierra/component "0.3.2"]
     [org.clojure/tools.namespace "0.2.11"]
     [hiccup "1.0.5"]
     [prismatic/schema "1.1.4"]
     [selmer "1.10.7"]
     [yada "1.2.2" :exclusions [aleph manifold ring-swagger prismatic/schema]]

     [clj-time "0.13.0"]

     ;; DB dependencies
     ;;[com.layerware/hugsql "0.4.7"]
     [org.clojure/java.jdbc "0.7.0-alpha3"  :scope "test"]
     ;;[com.h2database  "1.4.195"             :scope "test"]
     ;;[com.postgresql  "42.1.1"              :scope "test"]
     [atea/hikaricp-component "0.1.8"]
     [com.ibm.informix/jdbc "4.10.8.1"]
     [org.clojars.pntblnk/clj-ldap "0.0.12" :scope "test"]  ;; LDAP

     ;;[datascript "0.15.5"]

     ;;logging
     [org.clojure/tools.logging "0.3.1"]
     [org.slf4j/jcl-over-slf4j "1.7.21"]
     [org.slf4j/jul-to-slf4j "1.7.21"]
     [org.slf4j/log4j-over-slf4j "1.7.21"]
     [ch.qos.logback/logback-classic "1.1.5" :exclusions [org.slf4j/slf4j-api]]


     ;; App deps
     [reagent "0.6.1"]

     ;; Exceptions
     [dire "0.5.4"]
     [slingshot "0.12.2"]
   ]
 )

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-reload :refer [reload]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         '[adzerk.boot-test :refer [test]]
         '[pandeiro.boot-http :refer [serve]]
         '[crisptrutski.boot-cljs-test :refer [test-cljs report-errors!]]
         '[com.stuartsierra.component :as component]
         'clojure.tools.namespace.repl
         '[atea.system :refer [new-system]]
          )

;;(alter-var-root #'log/*logger-factory* (constantly (log-service/make-factory log-config)))

(def repl-port 5600)

(task-options!
 push {:ensure-branch nil}
 pom  {:project (symbol project)
       :version version
       :description "A sample boot project setup"
       :url "http://github.com/frap/bootdev"
       :scm {:url "http://github.com/frap/bootdev"}
       :license {"The MIT License (MIT)" "http://opensource.org/licenses/mit-license.php"}
       }
 test {:namespaces #{'atea.time-test }}
 test-cljs {:namespaces #{'atea.core }}
)

(def defaults {:test-dirs #{"test/cljc" "test/clj"}
               :output-to ["js/main"]
               :testbed :phantom
               :namespaces '#{atea.time-test
                              }
               })


(deftask testing
  []
  (merge-env! :source-paths #{"test/cljc" "test/clj"})
  identity)


(deftask add-source-paths
  "Add paths to :source-paths environment variable"
  [t dirs PATH #{str} ":source-paths"]
  (merge-env! :source-paths dirs)
  identity)

(deftask dev-system
  "Develop the server backend. The system is automatically started in
  the dev profile."
  []
  (let [run? (atom false)]
    (with-pass-thru _
      (when-not @run?
        (reset! run? true)
        (require 'reloaded.repl)
        (let [go (resolve 'reloaded.repl/go)]
          (try
            (require 'user)
            (go)
            (catch Exception e
              (boot.util/fail "Exception while starting the system\n")
              (boot.util/print-ex (.getCause e)))))))))

(deftask dev
  "This is the main development entry point."
  []
  (set-env! :dependencies #(vec (concat % '[[reloaded.repl "0.2.3"]])))
  (set-env! :source-paths #(conj % "dev"))

  ;; Needed by tools.namespace to know where the source files are
  (apply clojure.tools.namespace.repl/set-refresh-dirs (get-env :directories))

  (comp
   (watch)
   (speak)
   (cider)
   (repl :server true
         :port repl-port
         :init-ns 'user)
   (dev-system)
   (target)))

(deftask build
  []
  (target :dir #{"static"}))


(deftask tdd
  "Launch a customisable TDD Environment"
  [e testbed        ENGINE kw     "the JS testbed engine (default phantom)"
   k httpkit               bool   "Use http-kit web server (default jetty)"
   n namespaces     NS     #{sym} "the set of namespace symbols to run tests in"
   o output-to      NAME   str    "the JS output file name for test (default main.js)"
   O optimisations  LEVEL  kw     "the CLJS optimisation level (default none)"
   p port           PORT   int    "the web server port to listen on (default 3000)"
   t dirs           PATH   #{str} "test paths (default test/clj test/cljs test/cljc)"
   v verbose               bool   "Print which files have changed (default false)"]
  (let [dirs (or dirs (:test-dirs defaults))
        output-to (or output-to (:output-to defaults))
        testbed (or testbed (:testbed defaults))
        namespaces (or namespaces (:namespaces defaults))]
    (comp
     (serve :handler 'atea.system/new-system
            :resource-root "target"
            :reload true
            :httpkit httpkit
            :port port)
     (add-source-paths :dirs dirs)
     (watch :verbose verbose)
     (speak)
     (reload :ws-host "localhost")
     (cljs-repl)
     (test-cljs :ids output-to
                :js-env testbed
                :namespaces namespaces
                :update-fs? true
                :optimizations optimisations)
     (test :namespaces namespaces)
     (target :dir #{"target"}))))

(deftask clj-tdd
  "Launch a clj TDD Environment"
  []
  (comp
   (testing)
   (watch)
   (speak)
   (test :namespaces #{'atea.lib-test}
         )
   )
  )

(deftask cljs-tdd
  "Launch a cljs TDD Environment"
  []
  (comp
   (testing)
   (watch)
   (speak)
   (test-cljs :namespaces #{'atea.lib-test
                            'atea.app-test}
              :js-env :phantom)
   )
  )

(deftask test-watch-karma []
  (comp (testing)
        (watch)
        (speak)
        (test-cljs :js-env :chrome)
        (test)))

(deftask test-all []
  (comp (testing)
        (test-cljs :keep-errors? true)
        (test)
        (report-errors!)))

(deftask run-system
  [p profile VAL str "Profile to start system with"]
  (with-post-wrap fileset
    (println "Running system with profile" profile)
    (let [system (new-system profile)]
      (component/start system)
      (intern 'user 'system system)
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
