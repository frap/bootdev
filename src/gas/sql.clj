(ns gas.sql
  (:require [hugsql.core :as hugsql]))

;; wires up def-db-fns macro so that when we call it later
;; it will parse the uccx.sql file and create the fns for us
;; the  gas.sql namespace

(hugsql/def-db-fns "uccx.sql")
