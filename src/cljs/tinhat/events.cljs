(ns tinhat.events
  (:require
    [re-frame.core :as rf]
    [tinhat.db :as db]
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [day8.re-frame.http-fx]
    [ajax.core :as ajax]
    [tinhat.config :as config]))

(defn check-and-throw
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def check-spec-interceptor (rf/after (partial check-and-throw :todomvc.db/db)))

(defn dispatch-timer-event
  []
  (let [now (js/Date.)]
    (rf/dispatch [:timer now])))

(defonce do-timer (js/setInterval dispatch-timer-event 1000))

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

(defn get-time
  []
  (-> (js/Date.)
      .toTimeString
      (str/split " ")
      first))

(defn create-messages
  [message-set]
  (->> message-set
       (map #(conj % (random-uuid) (get-time)))
       (into [])))

; TODO: probably will need to use ordered-map at some point
(rf/reg-event-db
  ::initialize-db
  (fn [_ _]
    [check-spec-interceptor]
    (let [appdb db/default-db]
      (as-> appdb db
            (assoc db :chat-log {"Jimmy"  (-> [[:in "hi"]
                                               [:out "hello"]]
                                              create-messages)
                                 "Johnny" (-> [[:out "bye"]
                                               [:in "goodbye"]]
                                              create-messages)
                                 "Jamie"  (-> [[:out "message 1"]
                                               [:out "message 2"]
                                               [:in "message 3"]]
                                              create-messages)
                                 "Jammy"  (-> [[:out "blah"]
                                               [:in "message"]
                                               [:out "ah"]
                                               [:out "eh"]]
                                              create-messages)
                                 "Junie"  (-> [[:in "abasd"]
                                               [:out "What?"]
                                               [:in "akl;jk"]
                                               [:in "tesfdsg"]
                                               [:in "skdflsfjkd"]]
                                              create-messages)
                                 "Jerry"  (-> [[:out "<expletive>"]
                                               [:in (long-message)]]
                                              create-messages)})
            (assoc db :active-chat (first (keys (:chat-log db))))))))

(rf/reg-event-db
  :timer
  (fn [db [_ new-time]]
    (assoc db :time new-time)))

(rf/reg-event-db
  :time-color-change
  (fn [db [_ new-color-value]]
    (assoc db :time-color (str/upper-case new-color-value))))

(rf/reg-event-db
  :temp-time-color-change
  (fn [db [_ new-color-value]]
    (assoc db :temp-time-color (str/upper-case new-color-value))))

(rf/reg-event-db
  :name-change
  (fn [db [_ new-name]]
    (assoc db :name (str/capitalize new-name))))

(rf/reg-event-db
  :set-active-chat
  (fn [db [_ new-chat]]
    (assoc db :active-chat new-chat)))

(rf/reg-event-db
  :reset-field
  (fn [db [_ field]]
    (assoc db field (field db/default-db))))

(rf/reg-event-db
  :key-code
  (fn [db [_ key-code]]
    (assoc db :key-code key-code)))

(rf/reg-event-db
  :toggle-sidebar
  (fn [db [_ toggle]]
    (assoc db :toggle-sidebar? (not toggle))))

(rf/reg-event-db
  :add-recipient
  (fn [db [_ recipient]]
    (as-> recipient r
          (assoc (:chat-log db) r [])
          (assoc db :chat-log r))))

(defn add-message-to-db
  [db message]
  (->> message
       (conj (-> db
                 :chat-log
                 (get (:active-chat db))))
       (assoc (:chat-log db) (:active-chat db))
       (assoc db :chat-log)))

(defn post-params-create
  [db message tableName]
  (str
    "{
         \"operation\": \"create\",
         \"tableName\": \"" tableName "\",
         \"payload\": {
             \"Item\": {
                 \"uuid\": \"" (nth message 2) "\",
                  \"contact\": \"" (:active-chat db) "\",
                  \"message\": \"" (second message) "\",
                  \"time\": \"" (last message) "\",
                  \"direction\": \"" (-> message
                                         first
                                         str
                                         (str/replace-first ":" "")) "\"
              }
          }
      }"))

(defn post-params-query
  [contact tableName]
  (str
    "{
         \"operation\": \"query\",
         \"tableName\": \"" tableName "\",
         \"payload\": {
             \"KeyConditionExpression\": \"contact = :person\",
             \"ExpressionAttributeValues\": {
                 \":person\": \"" contact "\"
             },
             \"Limit\": 15
         }
     }"))

(defn map-to-vector
  [{:keys [GUID message time date direction]}]
  [(keyword direction) message GUID date time])

(rf/reg-event-db
  :assoc-messages
  (fn [db [_ message-set]]
    (->>
      (->> message-set
           :Items
           (map map-to-vector)
           (into []))
      (concat (-> db
                  :chat-log
                  (get (:contact (first message-set)))))
      (assoc (:chat-log db) (:contact (first message-set)))
      (assoc db :chat-log))))

(rf/reg-event-fx
  :send-message
  (fn [{:keys [db]} [_ message]]
    {:db         (add-message-to-db db message)
     :http-xhrio {:method          :post
                  :uri             config/api-url
                  :timeout         8000
                  :params          (post-params-create db
                                                       message
                                                       config/table-name)
                  :format          (ajax/text-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:good-http-result]
                  :on-failure      [:bad-http-result]}}))

(rf/reg-event-fx
  :get-messages
  (fn [{:keys [db]} [_ _]]
    {:http-xhrio {:method          :post
                  :uri             config/api-url
                  :timeout         8000
                  :params          (post-params-query (:active-chat db)
                                                      config/table-name)
                  :format          (ajax/text-request-format)
                  :response-format (ajax/json-response-format)
                  :on-success      [:assoc-message]
                  :on-failure      [:bad-http-result]}}))

(rf/reg-event-db
  :get-message
  (fn [db [_ _]]
    (let [message (rand-nth ["Hi"
                             "Hello"
                             "New phone who dis?"
                             "Stop contacting me"
                             "wyd?"])]
      (add-message-to-db db [:in message (random-uuid) (get-time)]))))