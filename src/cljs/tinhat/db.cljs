(ns tinhat.db
  (:require [cljs.spec.alpha :as s]))

(def default-db
  {:time            (js/Date.)
   :time-color      "#00A"
   :temp-time-color "#00A"
   :name            "World"
   :chat-log        nil
   :active-chat     nil
   :toggle-sidebar? true})