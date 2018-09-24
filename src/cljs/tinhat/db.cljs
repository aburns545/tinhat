(ns tinhat.db)

(def default-db
  {:time            (js/Date.)
   :chat-log        nil
   :active-chat     nil
   :toggle-sidebar? true})