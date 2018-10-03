(ns tinhat.payloads
  (:require [cljs-time.coerce :as coerce]
            [clojure.string :as str]
            [tinhat.util :as util]))

(defn get-contacts
  [tableName]
  (str
    "{
         \"operation\": \"list\",
         \"tableName\": \"" tableName "\",
         \"payload\": {
             \"Limit\": 20
         }
     }"))

; TODO: need to change contact to number to prevent collisions
(defn upload-message
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
                 \"message-index\": \"" (:message-index message) "\",
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

(defn get-messages
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