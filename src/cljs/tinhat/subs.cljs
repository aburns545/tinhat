(ns tinhat.subs
  (:require
    [re-frame.core :as rf]))

(rf/reg-sub
  :time
  (fn [db _]
    (:time db)))

(rf/reg-sub
  :message-index
  (fn [db _]
    (:message-index db)))

(rf/reg-sub
  :chat-log
  (fn [db _]
    (:chat-log db)))

(rf/reg-sub
  :active-chat
  (fn [db _]
    (:active-chat db)))

(rf/reg-sub
  :toggle-sidebar?
  (fn [db _]
    (:toggle-sidebar? db)))

(rf/reg-sub
  :loading-messages?
  (fn [db _]
    (:loading-messages? db)))

(rf/reg-sub
  :contacts
  (fn [db _]
    (:contacts db)))

(rf/reg-sub
  :reload-flag
  (fn [db _]
    (:reload-flag db)))