(ns gas.handlers
  (:require  [gas.db :refer [hr-spec]]
             [dire.core :refer [with-handler!]]))


(with-handler! #'hr-spec
  (fn [e config]
    (println (str  "exception" e))))
