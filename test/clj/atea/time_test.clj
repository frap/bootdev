(ns atea.time-test
  (:require [clojure.test :refer :all]
            [atea.time    :refer :all]
            )
  )


(def mock-calls (atom {}))
(defn stub-fn [the-function return-value]
  (swap! mock-calls assoc the-function [])
  (fn [& args]
    (swap! mock-calls update-in [the-function] conj args)
    return-value))
(defn mock-fn [the-function]
  (stub-fn the-function nil))

(defmacro mocking [fn-names & body]
  (let [mocks (map #(list `mock-fn (keyword %)) fn-names)]
    `(with-redefs [~@(interleave fn-names mocks)]
       ~@body)))

(defmacro stubbing [stub-forms & body]
  (let [stub-pairs (partition 2 stub-forms)
        real-fns (map first stub-pairs)
        returns (map last stub-pairs)
        stub-fns (map #(list `stub-fn %1 %2) real-fns returns)]
    `(with-redefs [~@(interleave real-fns stub-fns)]
       ~@body)))

(deftest test-simple-data-parsing
  (let [d (date "2012-01-25")]
    (is (= (day-from d) 25))))
