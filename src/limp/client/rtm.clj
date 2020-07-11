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

(defn- build-message-topology
  [socket & handlers]
  (let [base-stream
        (->> socket
             (ms/throttle 25 100)
             (ms/transform
              (comp

               ;; Convert Raw JSON to hashmap
               (map #(json/read-str % :key-fn keyword))

               ;; Filter out heartbeat messages

               ;; clj-ify types
               (map #(update % :type (comp keyword inflections/dasherize)))

               ;; Log type to STDOUT
               (map (fn [message]
                      (clojure.pprint/pprint message)
                      message)))))]

    ;; Creates an individual stream for each message handler
    (map (fn [[pred handler]]
           (ms/transform
            (comp
             (filter pred)
             (map handler))
            base-stream)) (partition-all 2 handlers))))

(defn- post-message!
  [socket {:keys [channel text]}]
  (ms/put! socket
           (json/write-str
            {:channel channel
             :id (get-message-id)
             :text text
             :type "message"})))

(defn- start-heartbeat
  [socket connected]
  (while true
    (let [result @(ms/put! socket
                           (json/write-str
                            {:id (get-message-id)
                             :type "ping"}))]
      (when-not result
        (reset! connected false))
      (Thread/sleep 5000))))

(defn start!
  [& handlers]
  (let [{body :body}
        @(http/get +slack-endpoint+
                   {:query-params {"token" (System/getenv "SLACK_TOKEN")}})

        {:keys [ok url]}
        (json/read-str (bs/to-string body) :key-fn keyword)

        connect-socket
        (fn []
          (when ok
            @(http/websocket-client url)))]

    (while true
      (let [socket (connect-socket)
            handler-streams (apply build-message-topology
                                   socket
                                   handlers)
            connected (atom true)
            heartbeat (future (start-heartbeat socket connected))]
        (try
          ;; Publish messages output by handler streams
          (try
            (while @connected
              (doseq [handler-stream handler-streams]
                (if-let [message @(ms/take! handler-stream)]
                  (post-message! socket message))))

            (finally (future-cancel heartbeat)))
          (finally (ms/close! socket)))))))
