(ns tinhat.views
  (:require
    [re-frame.core :as rf]
    [tinhat.subs :as subs]
    [reagent.core :as r]
    [cljsjs.react-bootstrap]
    [tinhat.util :as util]))

(defn message-input
  [temp-message]
  (fn []
    [:div
     [:input {:style        {:width "90%"
                             :color (if (= @temp-message "Text Message")
                                      "#555"
                                      "#000")}
              :type         "text"
              :value        @temp-message
              :on-focus     #(when (-> %
                                       .-target
                                       .-value
                                       (= "Text Message"))
                               (reset! temp-message ""))
              :on-blur      #(when (-> %
                                       .-target
                                       .-value
                                       (= ""))
                               (reset! temp-message "Text Message"))
              :on-change    #(reset! temp-message (-> %
                                                      .-target
                                                      .-value))
              :on-key-press #(when (-> @temp-message
                                       (not= "")
                                       (and (= "Enter" (.-key %))))
                               (rf/dispatch [:send-message
                                             (-> [:out
                                                  @temp-message
                                                  @(rf/subscribe [:message-index])]
                                                 util/create-message)])
                               (rf/dispatch [:inc-message-index])
                               (reset! temp-message ""))}]]))

(defn show-chat-log
  []
  (let [recipient @(rf/subscribe [:active-chat])
        messages (-> @(rf/subscribe [:chat-log])
                     (get recipient))]
    [:div {:style {:border "1px solid black"
                   :width  "90%"
                   :height "525px"}}
     [:h3 {:style {:background "#7386D5"}}
      @(rf/subscribe [:active-chat])]
     [:div {:style {:width      "100%"
                    :height     "481px"
                    :overflow-y "auto"}}
      (when (-> messages
                util/nil-or-empty?
                not)
        (for [message messages]
          [:div {:style {:width "100%"}
                 :key   (:uuid message)}
           [:table {:style (case (:direction message)
                             :out util/outbound-message-styling
                             :in util/inbound-message-styling)}
            [:tbody
             [:tr
              [:td {:style {:text-align (case (:direction message)
                                          :in "right"
                                          :out "left")
                            :fontSize   10}}
               (->> message
                    :datetime
                    util/get-time)]]
             [:tr
              [:td
               (:message message)]]]]]))
      (when @(rf/subscribe [:loading-messages?])
        [:div {:style {:width "100%"}}
         [:table {:style util/outbound-message-styling}
          [:tbody
           [:tr
            [:td {:style {:text-align "left"
                          :fontSize   "10"}}
             (util/get-current-time)]]
           [:tr
            [:td "loading..."]]]]])]]))

(defn content
  [temp-message]
  (fn []
    [:div
     [:section#content-section
      [show-chat-log]]
     [message-input temp-message]]))

(defn toggle-button
  []
  (let [toggle (r/atom true)]
    (fn []
      [:button {:type     "button"
                :id       "sidebarCollapse"
                :class    "btn btn-info"
                :on-click #(do
                             (rf/dispatch [:toggle-sidebar @toggle])
                             (swap! toggle not))}
       [:i {:class "fas fa-align-left"}]
       [:span "Toggle Sidebar"]])))

(defn get-message-button
  []
  [:button {:placeholder #(rf/dispatch [:get-message
                                        (-> [:in
                                             (rand-nth ["Hi"
                                                        "Hello"
                                                        "New phone who dis?"
                                                        "Stop contacting me"
                                                        "wyd?"])
                                             util/create-message])])
            :on-click    #(rf/dispatch [:get-messages])}
   [:label "Send me a message"]])

(defn toggle-sidebar
  []
  [:di {:id "content"}
   [:nav {:class "navbar navbar-expand-lg navbar-light bg-light"}
    [:div {:class "container-fluid"}
     [toggle-button]
     [get-message-button]]]])

; TODO: refactor this to work with new contact implementation
(defn create-conversation
  [temp-contact temp-message]
  [:input {:style        {:background  "#7386D5"
                          :borderColor "#7386D5"
                          :color       "#fff"}
           :type         "text"
           :value        @temp-contact
           :on-focus     #(reset! temp-contact "")
           :on-blur      #(reset! temp-contact "+ Create Conversation")
           :on-change    #(reset! temp-contact (-> %
                                                   .-target
                                                   .-value))
           :on-key-press #(when (= "Enter" (.-key %))
                            (rf/dispatch [:add-contact @temp-contact])
                            (rf/dispatch [:set-active-chat
                                          @temp-contact])
                            (reset! temp-contact
                                    "+ Create Conversation")
                            (reset! temp-message "Text Message"))}])

(defn show-contacts
  [temp-message]
  (for [contact-map @(rf/subscribe [:contacts])]
    (let [contact (:name contact-map)]
      [:li {:key contact}
       [:a {:on-click #(when (-> contact
                                 (not= @(rf/subscribe [:active-chat])))
                         (rf/dispatch [:set-active-chat contact])
                         (when (-> @(rf/subscribe [:chat-log])
                                   (get contact)
                                   util/nil-or-empty?)
                           (rf/dispatch [:get-messages])
                           (js/console.log (-> @(rf/subscribe [:chat-log])
                                               (get contact))))
                         (reset! temp-message "Text Message"))}
        [:label contact]]])))

(defn sidebar
  []
  (let [temp-message (r/atom "Text Message")
        new-contact (r/atom "+ Create Conversation")
        reload-flag @(rf/subscribe [:reload-flag])]
    (fn []
      (when reload-flag)
      [:div {:class "wrapper"}
       [:nav {:id    "sidebar"
              :class (if @(rf/subscribe [:toggle-sidebar?])
                       "inactive"
                       "active")}
        [:div {:class "sidebar-header"}
         [:h3 "Placeholder"]]
        [:ul {:class "list-unstyled components"}
         [:li
          [create-conversation new-contact temp-message]]
         (show-contacts temp-message)]]
       [:div {:id    "content"
              :style {:overflow "hidden"}}
        [toggle-sidebar]
        [content temp-message]]])))

(defn main-panel
  []
  [sidebar])
