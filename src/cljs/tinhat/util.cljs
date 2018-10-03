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
      (or empty?)))

(defn long-message
  []
  (str "What the jiminy crickets did you just flaming say about me,"
       " you little bozo? I’ll have you know I graduated top of my "
       "class in the Cub Scouts, and I’ve been involved in numerous "
       "secret camping trips in Wyoming, and I have over 300 confirmed "
       "knots. I am trained in first aid and I’m the top bandager in "
       "the entire US Boy Scouts (of America). You are nothing to me "
       "but just another friendly face. I will clean your wounds for "
       "you with precision the likes of which has never been seen "
       "before on this annual trip, mark my words. You think you can "
       "get away with saying those shenanigans to me over the Internet"
       "? Think again, finkle. As we speak I am contacting my secret "
       "network of MSN friends across the USA and your IP is being "
       "traced right now so you better prepare for the seminars, man. "
       "The storm that wipes out the pathetic little thing you call "
       "your bake sale. You’re frigging done, kid. I can be anywhere, "
       "anytime, and I can tie knots in over seven hundred ways, and "
       "that’s just with my bare hands. Not only am I extensively "
       "trained in road safety, but I have access to the entire manual "
       "of the United States Boy Scouts (of America) and I will use it "
       "to its full extent to train your miserable butt on the facts "
       "of the continents, you little schmuck. If only you could have "
       "known what unholy retribution your little “clever” comment was "
       "about to bring down upon you, maybe you would have held your "
       "silly tongue. But you couldn’t, you didn’t, and now you’re "
       "paying the price, you goshdarned sillyhead. I will throw "
       "leaves all over you and you will dance in them. You’re friggin "
       "done, kiddo."))

(defn default-test-data
  []
  (as-> db/default-db db
        (assoc db :chat-log {"Jimmy"  (-> [[:in "hi" 1]
                                           [:out "hello" 2]]
                                          create-messages)
                             "Johnny" (-> [[:out "bye" 3 ]
                                           [:in "goodbye" 4]]
                                          create-messages)
                             "Jamie"  (-> [[:out "message 1" 5]
                                           [:out "message 2" 6]
                                           [:in "message 3" 7]]
                                          create-messages)
                             "Jammy"  (-> [[:out "blah" 8]
                                           [:in "message" 9]
                                           [:out "ah" 10]
                                           [:out "eh" 11]]
                                          create-messages)
                             "Junie"  (-> [[:in "abasd" 12]
                                           [:out "What?" 1]
                                           [:in "akl;jk" 2]
                                           [:in "tesfdsg" 3]
                                           [:in "skdflsfjkd" 4]]
                                          create-messages)
                             "Jerry"  (-> [[:out "<expletive>" 5]
                                           [:in (long-message) 6]]
                                          create-messages)})
        (assoc db :contacts [{:name "Jimmy"}
                             {:name "Johnny"}
                             {:name "Jamie"}
                             {:name "Jammy"}
                             {:name "Junie"}
                             {:name "Jerry"}])
        (assoc db :active-chat (first (keys (:chat-log db))))))