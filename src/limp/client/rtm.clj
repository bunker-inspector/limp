(ns limp.client.rtm
  (:require [aleph.http :as http]
            [byte-streams :as bs]
            [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [inflections.core :as inflections]
            [manifold.stream :as ms]))

(def ^:const +slack-endpoint+ (or (System/getenv "SLACK_ENDPOINT")
                                  "https://slack.com/api/rtm.connect"))

(def session-message-id (atom 0))
(def get-message-id (partial swap! session-message-id inc))

(defrecord Response [channel text])

(defn- post-message!
  [socket {:keys [channel text]}]
  (ms/put! socket
           (json/write-str
            {:channel channel
             :id (get-message-id)
             :text text
             :type "message"})))

(defn start!
  [& handlers]
  (let [token
        (System/getenv "SLACK_TOKEN")

        {:keys [body]}
        @(http/get +slack-endpoint+
                   {:query-params {"token" token}})

        {:keys [ok url] :as body-parsed
         {:keys [self-id]} :self}
        (json/read-str (bs/to-string body) :key-fn keyword)

        socket
        (when ok
          (try
            @(http/websocket-client url)
            (catch Exception e
              (clojure.pprint/pprint e))))]
    (when socket
      (try
        (let [base-stream
              (->> socket
                   (ms/throttle 25 100)
                   (ms/transform
                    (comp

                     ;; Convert Raw JSON to hashmap
                     (map #(json/read-str % :key-fn keyword))

                     ;; Filter out heartbeat messages
                     (remove (fn [{type :type}] (= type "hello")))

                     ;; clj-ify types
                     (map #(update % :type (comp keyword inflections/dasherize)))

                     ;; Log type to STDOUT
                     (map (fn [message]
                            (clojure.pprint/pprint message)
                            message)))))

              ;; Creates an individual stream of each message handler
              handler-streams
              (map (fn [[pred handler]]
                     (ms/transform
                      (comp
                       (filter pred)
                       (map handler))
                      base-stream)) (partition-all 2 handlers))]

          ;; Publish messages output by handler streams
          (while true
            (doseq [handler-stream handler-streams]
              (if-let [message @(ms/take! handler-stream)]
                (post-message! socket message)))))
        (finally (ms/close! socket))))))
