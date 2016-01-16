(ns wedding.core
  (:require-macros  [cljs.core.async.macros :refer  [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html]
            [cljs.core.async :refer  [<!]]
            [cljs-http.client :as http]))

(enable-console-print!)

(defonce app-state (atom {:attendee ""
                          :email ""
                          :code ""
                          :guests "+0"
                          :going "true"
                          :message ""
                          :status 0
                          :page "home"
                          }))

(defn register
  [cursor]
  (go (let [resp (<! (http/post "api/rsvp" {:form-params @cursor}))]
        (om/update! cursor :status (:status resp))
        (om/update! cursor :message (:body resp)))))

(defn header
  [data]
  [:div.header.clearfix
     [:nav.navbar.navbar-default
      [:ul.nav.nav-pills
       [:li [:a {:href "#"
                 :on-click #(om/update! data :page "home")} "Invitation"]]
       [:li [:a {:href "#"
                 :on-click #(om/update! data :page "rsvp")
                 } "RSVP"]] ] ] ])

(defn invitation
  [data]
  (html/html
   [:div.container
    (header data)
    [:div.invitation
     [:img {:src "images/lights.png"}]
     [:h2 "The Wedding of"]
     [:h1 "Chris"]
     [:h2 "and"]
     [:h1 "Tammy"]
     [:div.row
      [:div.col-md-4
       [:img {:src "images/pot.png"}]
       ]
      [:div.col-md-8
       [:div.info
        [:img {:src "images/flowers.png"}]
        [:h4 "April 16"]
        [:h2 "2016"]
        [:h4 "11:00"]]
       [:div.info
        [:img {:src "images/flowers.png"}]
        [:h2 "The Little Chapel in the Woods"]
        [:h4
         "415 Chapel Dr"
         "Denton, Tx 76204"
         ]
        [:a {:href "https://www.google.com/maps/place/Little+Chapel+in+the+Woods/@33.2301368,-97.1305113,17.31z/data=!4m2!3m1!1s0x864dca8385accd75:0x21f147777f4eba8b"} [:span.glyphicon.glyphicon-map-marker]]
        ]

            [:div.info
             [:h2 "Social Mixer to Follow"] 
             [:a {:href "https://www.google.com/maps/place/Hannah's+Off+the+Square/@33.2132881,-97.1353291,17z/data=!3m1!4b1!4m2!3m1!1s0x864dca8b1e4c0ded:0xb1b338a249bbf5fa"} [:span.glyphicon.glyphicon-map-marker]]]]]]]))

(defn update-data
  [cursor ks]
  (fn [e]
    (om/update! cursor ks (.. e -target -value))))

(defn update-checked
  [cursor ks]
  (fn [e]
    (om/update! cursor ks (.. e -target -checked))))

(defn text-input
  [cursor ks]
  (let [n (name ks)]
    [:input.form-control {:type "text"
                          :name n
                          :id n
                          :on-change (update-data cursor ks)
                          :value (ks cursor)}]))

(defn rsvp
  [data]
  (html/html
   [:div.container
    (header data)
    [:div.rsvp
     [:div.well.form-horizontal.clearfix
      [:div.form-group
       [:div.col-sm-6 [:a.btn.btn-default.btn-block {:class (if (:going data) "active" "disabled")
                                                     :on-click #(om/update! data :going true)} "Attending"]]
       [:div.col-sm-6 [:a.btn.btn-default.btn-block {:class (if (:going data) "disabled" "active")
                                                     :on-click #(om/update! data :going false)} "Not Attending"]]]
      [:div.form-group
       [:label.col-sm-3.control-label {:for "attendee"} "Attendee Name"]
       [:div.col-sm-9 (text-input data :attendee)]
       ]
      [:div.form-group
       [:label.col-sm-3.control-label {:for "code"} "Invitation Code"]
       [:div.col-sm-9 (text-input data :code)]]
      [:div.form-group
       [:label.col-sm-3.control-label {:for "count"} "Number of Guests"]
       [:div.col-sm-9 [:select.form-control
                       {:name "guests"
                        :on-change (update-data data :guests)
                        :value (:guests data)
                        }
        (map #(-> [:option (str "+" %)]) (range 0 6)) ]]]
      [:div.form-group
       [:label.col-sm-3.control-label {:for "email"} "Email(Optional)"]
       [:div.col-sm-9 (text-input data :email)]]
      [:button.btn.btn-default.pull-right {:type "button"
                                           :on-click (fn [_] (register data))} "RSVP"]]
      (if (= (:status data) 200)
        [:div.alert.alert-success (:message data)]
        (if (= (:status data) 400)
          [:div.alert.alert-danger [:ul (map #(-> [:li %]) (:message data))]]))
      ]]))

(om/root
 (fn  [data owner]
   (reify om/IRender
     (render  [_]
       (case (:page data)
         "home"
         (invitation data)

         "rsvp"
         (rsvp data)))))
 app-state
 {:target  (. js/document  (getElementById "app"))})


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
