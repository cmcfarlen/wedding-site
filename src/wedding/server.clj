(ns wedding.server
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.middleware.params :as params]
            [ring.middleware.resource :as resource]
            [ring.middleware.reload :as reload]
            [clojure.edn :as edn]
            [ring.util.response :as response]))

(def invitation-code (.trim (slurp "invitation-code.txt")))
(def rsvp-storage "rsvp-list.edn")


(defn load-guest-list
  [file]
  (let [f (io/as-file file)]
    (if (.exists f)
      (edn/read-string (slurp f))
      #{})))

(defonce rsvp-list (atom (load-guest-list rsvp-storage)))

(defn save-watcher
  [_ _ _ guest-list]
  (spit rsvp-storage (pr-str guest-list)))

(add-watch rsvp-list :save save-watcher)

(defn validate-attendee
  [issues attendee]
  (if (empty? attendee)
    (conj issues "Attendee name cannot be empty.")
    issues))

(defn validate-email
  [issues email]
  issues)

(defn validate-code
  [issues code]
  (if (= code invitation-code)
    issues
    (conj issues "The invitation code is not correct")))

(defn validate-guests
  [issues guests]
  (if (re-matches #"\+\d+" guests)
    issues
    (conj issues "Invalid number of guests")))

(defn attendance-count
  [guests going]
  (if (= going "true")
    (inc (edn/read-string guests))
    0))

(defn add-quotes
  [s]
  (str \" s \"))

(defn as-csv
  [guests]
  (->> guests
       (group-by first)
       vals
       (map #(last (sort-by last %)))
       (map #(->> %
                  (map add-quotes)
                  (clojure.string/join ",")))
       (clojure.string/join "\n")))

(defroutes rsvp
  (GET "/guests" []
    (pr-str @rsvp-list))
  (GET "/guests.csv" []
    (let [csv (as-csv @rsvp-list)]
      (-> (response/response csv)
          (response/content-type "application/csv"))))
  (POST "/rsvp" [attendee email code guests going :as req]
        (let [issues (-> []
                         (validate-attendee attendee)
                         (validate-email email)
                         (validate-code code)
                         (validate-guests guests))]
          (if (empty? issues)
            (do
             (swap! rsvp-list conj [attendee email code (attendance-count guests going) going (java.util.Date.)])
             (if (= going "true")
               (str "Thanks " attendee "! See you there!")
               (str "Thank you for responding. We are sorry you cannot attend.")))
            (-> (response/response (pr-str issues))
                (response/content-type "application/edn")
                (response/status 400))))))

(def handler (-> rsvp
                 params/wrap-params))

#_(def handler (-> rsvp
                 params/wrap-params
                 (resource/wrap-resource "public")
                 (reload/wrap-reload)))
