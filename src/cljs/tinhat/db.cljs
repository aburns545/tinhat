(ns tinhat.db)

(def default-db
  {:time              (js/Date.)
   :chat-log          {}
   :active-chat       ""
   :contacts          []
   :toggle-sidebar?   true
   :loading-messages? false
   :message-index     0
   :need-messages?    true
   :reload-flag       true})