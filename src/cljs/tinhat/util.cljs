(ns tinhat.util
  (:require
    [clojure.string :as str]))


(defn get-time
  []
  (-> (js/Date.)
      .toTimeString
      (str/split " ")
      first))

(defn get-date
  []
  (-> (js/Date.)
      .toDateString
      (str/split " ")
      first))

(defn create-messages
  [message-set]
  (->> message-set
       (map #(conj % (random-uuid) (get-date) (get-time)))
       (into [])))