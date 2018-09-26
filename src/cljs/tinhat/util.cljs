(ns tinhat.util
  (:require
    [clojure.string :as str]
    [cljs-time.coerce :as coerce]
    [cljs-time.local :as local]
    [cljs-time.core :as time]))

(defn format-time
  [time]
  (let [hours (-> time
                  (subs 0 2)
                  int)]
    (str (if (> hours 12)
           (- hours 12)
           hours)
         ":"
         (subs time 2)
         (if (or (< hours 12) (= hours 24))
           "am"
           "pm"))))

(defn get-time
  [datetime]
  (->>
    (-> datetime
        str
        (str/split #"T"))
    second
    (drop-last 2)
    str/join
    format-time))

(defn get-current-time
  []
  (get-time (time/now)))

"Expects a vector of vectors containing a direction and a message i.e.
[:out \"message\"]"
(defn create-messages
  [message-set]
  (->> message-set
       (map #(hash-map :direction (first %)
                       :message (second %)
                       :uuid (random-uuid)
                       :datetime (time/now)))
       (into [])))

(defn create-message
  [message-params]
  {:direction (first message-params)
   :message   (second message-params)
   :uuid      (random-uuid)
   :datetime  (time/now)})

(defn keywordize
  [message-map]
  {:direction (keyword (get message-map "direction"))
   :message   (get message-map "message")
   :uuid      (get message-map "uuid")
   :datetime  (coerce/from-string (get message-map "datetime"))})