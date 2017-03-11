(ns gas.time
  (:require    [clj-time.core :refer [days minus today]]
               [clj-time.coerce :refer [to-sql-date]]
))

(def yesterday (-> (today) (minus (days 1)) to-sql-date))

(def now (.getTime (java.util.Date.)))

;;(def date-format (ctime-f/formatters :date))
;;(def prettydate-format (ctime-f/formatter "EEE d, yyyy 'at' HH.mm. "))

(def today (atom nil))

;;(defn update-today []
  (reset! today (ctime-f/unparse date-format (ctime-u/now))))

;;(defn print-today []
  (ctime-f/unparse (ctime-f/with-zone prettydate-format (ctime-u/default-time-zone)) (ctime-u/now)))
