(ns tinhat.subs
  (:require
    [re-frame.core :as rf]))

(rf/reg-sub
  :time
  (fn [db _]
    (:time db)))

(rf/reg-sub
  :time-color
  (fn [db _]
    (:time-color db)))

(rf/reg-sub
  :temp-time-color
  (fn [db _]
    (:temp-time-color db)))

(rf/reg-sub
  :chat-log
  (fn [db _]
    (:chat-log db)))

(rf/reg-sub
  :active-chat
  (fn [db _]
    (:active-chat db)))

(rf/reg-sub
  :key-code
  (fn [db _]
    (:key-code db)))

(rf/reg-sub
  :toggle-sidebar?
  (fn [db _]
    (:toggle-sidebar? db)))