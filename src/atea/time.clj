(ns atea.time
  (:require    [clj-time.core :refer [days minus today]]
               [clj-time.coerce :refer [to-sql-date]]
               )
  (:import (java.text SimpleDateFormat)
           (java.util Calendar GregorianCalendar)
           )
  )

(def yesterday (-> (today) (minus (days 1)) to-sql-date))

(def now (.getTime (java.util.Date.)))

(defn date [date-string]
  (let [f (SimpleDateFormat. "yyyy-MM-dd")
        d (.parse f date-string)]
    (doto (GregorianCalendar.)
      (.setTime d))))

(defn day-from [d]
  (.get d Calendar/DAY_OF_MONTH))

;;(def date-format (ctime-f/formatters :date))
;;(def prettydate-format (ctime-f/formatter "EEE d, yyyy 'at' HH.mm. "))

;;(def today (atom nil))

;;(defn update-today []
;;  (reset! today (ctime-f/unparse date-format (ctime-u/now))))

;;(defn print-today []
;;  (ctime-f/unparse (ctime-f/with-zone prettydate-format (ctime-u/default-time-zone)) (ctime-u/now)))
