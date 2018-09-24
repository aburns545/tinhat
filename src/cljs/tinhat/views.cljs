(ns tinhat.views
  (:require
    [re-frame.core :as rf]
    [tinhat.subs :as subs]
    [clojure.string :as str]
    [reagent.core :as r]
    [cljsjs.react-bootstrap]
    [clojure.string :as str]
    [tinhat.events :as events]))


(def table (r/adapt-react-class (.-Table js/ReactBootstrap)))

(def button (r/adapt-react-class (.-Button js/ReactBootstrap)))

(defn clock
  []
  [:div.test
   {:style {:color @(rf/subscribe [:time-color])}}
   (-> @(rf/subscribe [:time])
       .toTimeString
       (str/split " ")
       first)])

(defn color-input
  []
  [:div.color-input
   {:style {:color @(rf/subscribe [:time-color])}}
   "Time color: "
   [:input {:type         "text"
            :value        @(rf/subscribe [:temp-time-color])
            :on-change    #(rf/dispatch [:temp-time-color-change (-> %
                                                                     .-target
                                                                     .-value)])
            :on-submit    #(rf/dispatch [:time-color-change
                                         (-> %
                                             .-target
                                             .-value)])
            :on-key-press #(when (= "Enter" (.-key %))
                             (rf/dispatch [:time-color-change
                                           (-> %
                                               .-target
                                               .-value)]))}]
   [button {:on-click #(rf/dispatch [:time-color-change
                                     @(rf/subscribe [:temp-time-color])])}
    [:label "Submit"]]])

(defn name-input
  []
  [:div.name-input
   "Name? "
   [:input {:type      "text"
            :value     @(rf/subscribe [::subs/name])
            :on-change #(rf/dispatch [:name-change (-> %
                                                       .-target
                                                       .-value)])}]])

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
                               (rf/dispatch [:send-message [:out
                                                            @temp-message
                                                            (random-uuid)
                                                            (events/get-time)]])
                               (reset! temp-message ""))}]]))

(defn show-chat-log
  []
  (let [recipient @(rf/subscribe [:active-chat])
        messages (-> @(rf/subscribe [:chat-log])
                     (get recipient ["Invalid recipient"]))]
    [:div {:style {:border     "1px solid black"
                   :width      "90%"
                   :height     "525px"
                   :overflow-y "auto"}}
     [:h3 {:style {:background "#7386D5"
                   :padding    "5px"}}
      @(rf/subscribe [:active-chat])]
     (for [message messages]
       [:div {:style {:width "100%"}}
        [:table {:style (case (first message)
                          :out {:background    "#afa"
                                :float         "right"
                                :padding       "10px"
                                :margin-left   "50%"
                                :margin-bottom "10px"}
                          :in {:background    "#ddd"
                               :float         "left"
                               :padding       "10px"
                               :margin-right  "50%"
                               :margin-bottom "10px"})}
         [:tr
          [:td {:style {:text-align (case (first message)
                                      :in "right"
                                      :out "left")
                        :fontSize   "10"}}
           (->> message
                last
                (drop-last 3))]]
         [:tr
          [:td
           (second message)]]]])]))

(defn hello-world-stuff
  []
  [:h1 [clock]]
  [name-input]
  [color-input])

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
  [:button {:on-click #(rf/dispatch [:get-message])}
   [:label "Send me a message"]])

(defn toggle-sidebar
  []
  [:di {:id "content"}
   [:nav {:class "navbar navbar-expand-lg navbar-light bg-light"}
    [:div {:class "container-fluid"}
     [toggle-button]
     [get-message-button]]]])

(defn create-conversation
  [temp-target temp-message]
  [:input {:style        {:background  "#7386D5"
                          :borderColor "#7386D5"
                          :color       "#fff"}
           :type         "text"
           :value        @temp-target
           :on-focus     #(reset! temp-target "")
           :on-blur      #(reset! temp-target "+ Create Conversation")
           :on-change    #(reset! temp-target (-> %
                                                  .-target
                                                  .-value))
           :on-key-press #(when (= "Enter" (.-key %))
                            (rf/dispatch [:add-recipient @temp-target])
                            (rf/dispatch [:set-active-chat
                                          @temp-target])
                            (reset! temp-target
                                    "+ Create Conversation")
                            (reset! temp-message "Text Message"))}])

(defn show-recipients
  [temp-message]
  (for [recipient (keys @(rf/subscribe [:chat-log]))]
    [:li
     [:a {:on-click #(when (-> recipient
                               (not= @(rf/subscribe [:active-chat])))
                       (rf/dispatch [:set-active-chat recipient])
                       (reset! temp-message "Text Message"))}
      [:label recipient]]]))

(defn sidebar
  []
  (let [temp-message (r/atom "Text Message")
        temp-target (r/atom "+ Create Conversation")]
    (fn []
      [:div {:class "wrapper"}
       [:nav {:id    "sidebar"
              :class (if @(rf/subscribe [:toggle-sidebar?])
                       "inactive"
                       "active")}
        [:div {:class "sidebar-header"}
         [:h3 "Placeholder"]]
        [:ul {:class "list-unstyled components"}
         [:li
          [create-conversation temp-target temp-message]]
         (show-recipients temp-message)]]
       [:div {:id    "content"
              :style {:overflow "hidden"}}
        [toggle-sidebar]
        [content temp-message]]])))
(defn main-panel
  []
  [sidebar])
