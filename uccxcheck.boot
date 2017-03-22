#!/usr/bin/env boot

(set-env!
 :source-paths    #{"src"}
 :resource-paths  #{"resources"}
 :dependencies
 '[
   [reloaded.repl "0.2.3" :scope "test"]

   [org.clojure/clojure "1.9.0-alpha14"]
   [org.clojure/tools.nrepl "0.2.12" :scope "test"]
   [adzerk/bootlaces "0.1.13" scope "test"]

   ;; Server deps
   ;;[aero "1.0.3"]
   [aleph "0.4.3"]
   [com.stuartsierra/component "0.3.2"]
   [org.clojure/tools.namespace "0.2.11"]
   ;; DB dependencies
   ;;[com.layerware/hugsql "0.4.7"]
   [org.clojure/java.jdbc "0.7.0-alpha1"]
   [duct/hikaricp-component "0.1.0"]
   [com.informix/ifxjdbc "4.10.JC8DE"]
   ;;[local/ojdbc6 "11.2.0.4"]
   ;;logging
   [org.clojure/tools.logging "0.3.1"]
   ;;[adzerk/boot-logservice "1.2.0"]
   ;; Exceptions
   [dire "0.5.4"]
   ])

;;; Components

(require '[adzerk.bootlaces   :refer :all]
         '[clojure.java.io    :as io]
         '[boot.cli           :refer [defclifn]]
         '[gas.uccx           :refer [new-uccx-system]]
         )

;;(def +version+ "0.1.2")
;;(bootlaces! +version+)


(defclifn -main
"  Checks UCCX wallboarduser connection is setup correctly for
   Ateas wallboard and Cisco UCCX authentication tokens.

   UCCX variables (UCCX_SRV, UCCX_IP) can be set using command-line
   options below.

   The wallboarduser password can be set using the command-line options
   below, or via the UCCX_PASS environment variable.

   USAGE: uccx-check [options] "
      [s server NAME str "the UCCX server name (replace - with _)"
       i hostip IP str "the UCCX server ip"
       p passwd PASS str "the UCCX wallboard user password"
       ]
      (let [uccx-secondary (get *args* 0 "9.1.1.64")
            passwd   (or (System/getenv "UCCX_PASS") (:passwd *opts*))
            server   (or (System/getenv "UCCX_SRV")  (:server *opts*))
            hostip   (or (System/getenv "UCCX_IP")   (:hostip *opts*))
            ]

        (if (or (nil? server) (nil? hostip) (nil? passwd))
          (do  (println "\nERROR: you must provide UCCX server/IP and wallboard password credentials. Try uccx-check -h")
               (println "\nCurrent provided named parameters: " *opts*)
               )
          (do  (println  "\n** Getting stats from server = " server " @ IP=" hostip)
               (new-uccx-system server hostip passwd)

               )
          )
        )
  )
