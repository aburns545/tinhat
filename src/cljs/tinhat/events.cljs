(ns tinhat.events
  (:require
    [re-frame.core :as rf]
    [tinhat.db :as db]
    [clojure.string :as str]
    [day8.re-frame.http-fx]
    [ajax.core :as ajax]
    [tinhat.config :as config]
    [tinhat.util :as util]
    [cljs-time.core :as time]
    [cljs-time.coerce :as coerce]))

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

; TODO: probably will need to use ordered-map at some point
(rf/reg-event-db
  ::initialize-db
  (fn [_ _]
    (as-> db/default-db db
          (assoc db :chat-log {"Jimmy"  (-> [[:in "hi"]
                                             [:out "hello"]]
                                            util/create-messages)
                               "Johnny" (-> [[:out "bye"]
                                             [:in "goodbye"]]
                                            util/create-messages)
                               "Jamie"  (-> [[:out "message 1"]
                                             [:out "message 2"]
                                             [:in "message 3"]]
                                            util/create-messages)
                               "Jammy"  (-> [[:out "blah"]
                                             [:in "message"]
                                             [:out "ah"]
                                             [:out "eh"]]
                                            util/create-messages)
                               "Junie"  (-> [[:in "abasd"]
                                             [:out "What?"]
                                             [:in "akl;jk"]
                                             [:in "tesfdsg"]
                                             [:in "skdflsfjkd"]]
                                            util/create-messages)
                               "Jerry"  (-> [[:out "<expletive>"]
                                             [:in (long-message)]]
                                            util/create-messages)})
          (assoc db :active-chat (first (keys (:chat-log db)))))))

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
  :add-contact
  (fn [db [_ contact]]
    (as-> contact r
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
                 \"uuid\": \"" (:uuid message) "\",
                  \"contact\": \"" (:active-chat db) "\",
                  \"message\": \"" (:message message) "\",
                  \"datetime\": \"" (-> message
                                        :datetime
                                        (coerce/to-string)) "\",
                  \"direction\": \"" (-> message
                                         :direction
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
             \"Limit\": 20
         }
     }"))

(rf/reg-event-db
  :assoc-messages
  (fn [db [_ message-set]]
    (let [messages (get message-set "Items")]
      (->
        (->> messages
             (map util/keywordize)
             (into [])
             ; TODO: will definitely have to add another ordering mechanism
             (sort-by :datetime time/after?)
             (concat (-> db
                         :chat-log
                         (get (-> messages
                                  first
                                  (get "contact")))))
             (assoc (:chat-log db) (-> messages
                                       first
                                       (get "contact")))
             (assoc db :chat-log))
        (assoc :loading-messages? false)))))

(def default-api-params
  {:method          :post
   :uri             config/api-url
   :headers         {:Authorization "Basic"}
   :timeout         8000
   :params          nil
   :format          (ajax/text-request-format)
   :response-format (ajax/json-response-format)
   :on-success      [:good-http-result]
   :on-failure      [:bad-http-result]})

(rf/reg-event-fx
  :upload-messages
  (fn [{:keys [db]} [_ contact]]
    {:http-xhrio (->>
                   (-> db
                       :chat-log
                       (get contact))
                   (map #(assoc default-api-params
                           :params (post-params-create db
                                                       %
                                                       config/table-name)))
                   (into []))}))

(rf/reg-event-fx
  :send-message
  (fn [{:keys [db]} [_ message]]
    {:db         (add-message-to-db db message)
     :http-xhrio (assoc default-api-params
                   :params (post-params-create db
                                               message
                                               config/table-name))}))

; pulls up to 20 messages between the user and contact
(rf/reg-event-fx
  :get-messages
  (fn [{:keys [db]} [_ _]]
    {:db         (assoc db :loading-messages? true)
     :http-xhrio (assoc default-api-params
                   :params (post-params-query (:active-chat db)
                                              config/table-name)
                   :on-success [:assoc-messages])}))

; adds a generated message from the contact to the app-db and uploads it to the
; DynamoDB table
(rf/reg-event-fx
  :get-message
  (fn [{:keys [db]} [_ message]]
    {:db         (add-message-to-db db message)
     :http-xhrio (assoc default-api-params
                   :params (post-params-create db
                                               message
                                               config/table-name))}))