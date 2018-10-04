(ns tinhat.util
  (:require
    [clojure.string :as str]
    [cljs-time.coerce :as coerce]
    [cljs-time.local :as local]
    [cljs-time.core :as time]
    [tinhat.db :as db]))

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
  (get-time (local/local-now)))

"Expects a vector of vectors containing a direction and a message i.e.
 [:out \"message\"]"
(defn create-messages
  [message-set]
  (->>
    (for [[direction message message-index] message-set]
      {:direction     direction
       :message       message
       :uuid          (random-uuid)
       :datetime      (local/local-now)
       :message-index message-index})
    (into [])))

; TODO: Need to convert contact to number to prevent bucket collisions
(defn create-message
  [message-params]
  {:direction     (first message-params)
   :message       (second message-params)
   :uuid          (random-uuid)
   :datetime      (local/local-now)
   :message-index (last message-params)})

(defn keywordize
  [message-map]
  {:message       (get message-map "message")
   :uuid          (get message-map "uuid")
   :message-index (-> message-map
                      (get "message-index")
                      int)
   :direction     (-> message-map
                      (get "direction")
                      keyword)
   :datetime      (-> message-map
                      (get "datetime")
                      coerce/from-string
                      time/to-default-time-zone)})

(defn convert-contact
  [contact]
  {:number (get contact "number")
   :last-sent (-> contact
                  (get "last-sent")
                  coerce/from-long)
   :name (get contact "name")})

(defn dump-n-pass
  [data]
  (js/console.log data)
  data)

(defn nil-or-empty?
  [value]
  (-> value
      nil?
      (or (empty? value))))

(def outbound-message-styling
  {:background    "#afa"
   :float         "right"
   :padding       "10px"
   :margin-left   "50%"
   :margin-bottom "10px"})

(def inbound-message-styling
  {:background    "#ddd"
   :float         "left"
   :padding       "10px"
   :margin-right  "50%"
   :margin-bottom "10px"})