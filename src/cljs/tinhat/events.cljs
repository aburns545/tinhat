(ns tinhat.events
  (:require
    [re-frame.core :as rf]
    [tinhat.db :as db]
    [day8.re-frame.http-fx]
    [ajax.core :as ajax]
    [tinhat.config :as config]
    [tinhat.util :as util]
    [tinhat.payloads :as payload]
    [cljs-time.core :as time]))

(defn dispatch-timer-event
  []
  (let [now (js/Date.)]
    (rf/dispatch [:timer now])))

(defonce do-timer (js/setInterval dispatch-timer-event 1000))

(rf/reg-event-db
  :timer
  (fn [db [_ new-time]]
    (assoc db :time new-time
              :message-index 0)))

(rf/reg-event-db
  :inc-message-index
  (fn [db _]
    (->> db
         :message-index
         inc
         (assoc db :message-index))))

;TODO: Need to come up with proper error handling
(def default-api-params
  {:method          :post
   :uri             config/api-url
   :headers         {:Authorization "Basic Og=="}
   :timeout         8000
   :params          nil
   :format          (ajax/text-request-format)
   :response-format (ajax/json-response-format)
   :on-success      [:good-http-result]
   :on-failure      [:log-error]})

(rf/reg-event-fx
  :add-contacts
  (fn [{:keys [db]} [_ payload]]
    (let [contacts (as-> payload p
                         (get p "Items")
                         (map util/convert-contact p)
                         (into [] p)
                         (sort-by :last-sent time/after? p))]
      {:db         (-> db
                       (assoc :contacts contacts
                              :active-chat (-> contacts
                                               first
                                               :name)
                              :reload-flag (not (:reload-flag db))))
       :http-xhrio (assoc default-api-params
                     :params (payload/get-messages (-> contacts
                                                       first
                                                       :name)
                                                   config/message-table)
                     :on-success [:assoc-messages])})))

(def get-contacts
  (assoc default-api-params
    :params (payload/get-contacts config/contact-table)
    :on-success [:add-contacts]
    :on-failure [:log-error]))

(defn get-messages
  [db]
  (assoc default-api-params
    :params (payload/get-messages (:active-chat db)
                                  config/message-table)
    :on-success [:assoc-messages]))

; TODO: Potential areas of failure start here
(rf/reg-event-fx
  ::initialize-db
  (fn [{:keys [db]} _]
    {:db         (merge db db/default-db)
     :http-xhrio (assoc default-api-params
                   :params (payload/get-contacts config/contact-table)
                   :on-success [:add-contacts]
                   :on-failure [:log-error])}))

(rf/reg-event-fx
  :get-contacts
  (fn [_ _]
    {:http-xhrio (assoc default-api-params
                   :params (payload/get-contacts config/contact-table)
                   :on-success [:add-contacts]
                   :on-failure [:log-error])}))

(rf/reg-event-db
  :log-error
  (fn [_ [_ error]]
    (js/console.log error)))

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
  :toggle-reload-flag
  (fn [db [_ _]]
    (assoc db :reload-flag (not (:reload-flag db)))))

(rf/reg-event-db
  :set-message-need
  (fn [db [_ message-need]]
    (assoc db :need-messages? message-need)))

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

(rf/reg-event-db
  :assoc-messages
  (fn [db [_ message-set]]
    (let [messages (get message-set "Items")]
      (->
        (->> messages
             (map util/keywordize)
             (into [])
             (concat (-> db
                         :chat-log
                         (get (-> messages
                                  first
                                  (get "contact")))))
             (sort-by :message-index)
             (sort-by :datetime time/before?)
             (into [])
             (assoc (:chat-log db) (-> messages
                                       first
                                       (get "contact")))
             (assoc db :chat-log))
        (assoc :loading-messages? false
               :reload-flag (not (:reload-flag db)))))))

(rf/reg-event-fx
  :upload-messages
  (fn [{:keys [db]} [_ contact]]
    {:http-xhrio (->>
                   (-> db
                       :chat-log
                       (get contact))
                   (map #(assoc default-api-params
                           :params (->> config/message-table
                                        (payload/upload-message db %))))
                   (into []))}))

(rf/reg-event-fx
  :send-message
  (fn [{:keys [db]} [_ message]]
    {:db         (add-message-to-db db message)
     :http-xhrio (assoc default-api-params
                   :params (payload/upload-message db
                                                   message
                                                   config/message-table))}))

; pulls up to 20 messages between the user and contact
(rf/reg-event-fx
  :get-messages
  (fn [{:keys [db]} [_ _]]
    {:db         (assoc db :loading-messages? true)
     :http-xhrio (assoc default-api-params
                   :params (payload/get-messages (:active-chat db)
                                                 config/message-table)
                   :on-success [:assoc-messages])}))

; adds a generated message from the contact to the app-db and uploads it to the
; DynamoDB table
(rf/reg-event-fx
  :get-message
  (fn [{:keys [db]} [_ message]]
    {:db         (add-message-to-db db message)
     :http-xhrio (assoc default-api-params
                   :params (payload/upload-message db
                                                   message
                                                   config/message-table))}))