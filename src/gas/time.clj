(ns gas.time
  (:require    [clj-time.core :refer [days minus today]]
               [clj-time.coerce :refer [to-sql-date]]
))

(def yesterday (-> (today) (minus (days 1)) to-sql-date))

(def now (.getTime (java.util.Date.)))
