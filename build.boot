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
(def version "0.0.2" ;;(deduce-version-from-git)
  )

(set-env!
 :source-paths    #{"src"}
 :resource-paths  #{"resources"
                    "src" ;; add sources to uberjar
                    }
 :repositories #(conj % '["ibm" {:url "https://mvnrepository.com/artifact/com.ibm.informix/jdbc"}])
 :dependencies
 '[  [org.clojure/clojure "1.8.0"]         ;; add CLJ
     ;;[org.clojure/core.incubator "0.1.4"]
     [org.clojure/clojurescript "1.9.473"] ;; add CLJS

     [adzerk/boot-cljs "2.0.0"      :scope "test"]
     [pandeiro/boot-http "0.8.0"        :scope "test"]
     [adzerk/boot-reload "0.5.1"        :scope "test"]
     [adzerk/boot-cljs-repl "0.3.3"     :scope "test"]    ;; add bREPL
     [com.cemerick/piggieback "0.2.1"   :scope "test"]    ;; needed by bREPL
     [weasel "0.7.0"                    :scope "test"]    ;; websocket srv
     [org.clojure/tools.nrepl "0.2.12"  :scope "test"]    ;; needed by bREPL
     [reloaded.repl "0.2.3"             :scope "test"]
     [boot-deps "0.1.6"                 :scope "test"]    ;;  ancient for boot 
   
     ;;[deraen/boot-sass "0.3.0" :scope "test"]
     ;;TESTing
     [adzerk/boot-test "1.2.0"          :scope "test"]
     [crisptrutski/boot-cljs-test "0.2.1-SNAPSHOT" :scope "test"]

      
     ;; Server deps
     [aero "1.1.2"]
     [bidi "2.0.17"]
     [aleph "0.4.4-alpha3"]
     [com.stuartsierra/component "0.3.2"]
     [org.clojure/tools.namespace "0.2.11"]
     ;;[hiccup "1.0.5"]
     
     ;;[selmer "1.10.6"]
     [yada "1.2.2" :exclusions [aleph manifold ring-swagger prismatic/schema]]
   
     [clj-time "0.13.0"]

     ;; DB dependencies
     ;;[com.layerware/hugsql "0.4.7"]
     [org.clojure/java.jdbc "0.7.0-alpha3"  :scope "test"]
     ;;[com.h2database  "1.4.195"             :scope "test"]
     ;;[com.postgresql  "42.1.1"              :scope "test"]
     [atea/hikaricp-component "0.1.6"]
     [com.ibm.informix/jdbc "4.10.8.1"]
     [org.clojars.pntblnk/clj-ldap "0.0.12" :scope "test"]  ;; LDAP
   
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

(require '[adzerk.boot-cljs :refer [cljs]]
         '[pandeiro.boot-http :refer [serve]]
         '[adzerk.boot-reload :refer [reload]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         '[adzerk.boot-test :refer [test]]
         '[crisptrutski.boot-cljs-test :refer [test-cljs]]
         '[com.stuartsierra.component :as component]
         'clojure.tools.namespace.repl
         '[atea.system :refer [new-system]]
         '[org.clojure/tools.logging "0.3.1"]
         '[adzerk/boot-logservice "1.2.0"]
         )


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

(alter-var-root #'log/*logger-factory* (constantly (log-service/make-factory log-config)))

(def repl-port 5600)

(task-options!
 pom {:project (symbol project)
      :version version
      :description "A sample boot project setup"
      :license {"The MIT License (MIT)" "http://opensource.org/licenses/mit-license.php"}}
)

(def defaults {:test-dirs #{"test/cljc"}
               :output-to "main.js"
               :testbed :phantom
               :namespaces '#{atea.validators-test
                              atea.login.validators-test}})

(deftask add-source-paths
  "Add paths to :source-paths environment variable"
  [t dirs PATH #{str} ":source-paths"]
  (merge-env! :source-paths dirs)
  identity)

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


(deftask tdd
  "Launch a customisable TDD Environment"
  [e testbed        ENGINE kw     "the JS testbed engine (default phantom)"
   k httpkit               bool   "Use http-kit web server (default jetty)"
   n namespaces     NS     #{sym} "the set of namespace symbols to run tests in"
   o output-to      NAME   str    "the JS output file name for test (default main.js)"
   O optimizations  LEVEL  kw     "the CLJS optimisation level (default none)"
   p port           PORT   int    "the web server port to listen on (default 3000)"
   t dirs           PATH   #{str} "test paths (default test/clj test/cljs test/cljc)"
   v verbose               bool   "Print which files have changed (default false)"]
  (let [dirs (or dirs (:test-dirs defaults))
        output-to (or output-to (:output-to defaults))
        testbed (or testbed (:testbed defaults))
        namespaces (or namespaces (:namespaces defaults))]
    (comp
     (serve :handler 'atea.core/app
            :resource-root "target"
            :reload true
            :httpkit httpkit
            :port port)
     (add-source-paths :dirs dirs)
     (watch :verbose verbose)
     (reload :ws-host "localhost")
     (cljs-repl)
     (test-cljs :out-file output-to
                :js-env testbed
                :namespaces namespaces
                :update-fs? true
                :optimizations optimizations)
     (test :namespaces namespaces)
     (target :dir #{"target"}))))

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
