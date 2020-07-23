(ns limp.client.rtm
  (:require [aleph.http :as http]
            [byte-streams :as bs]
            [clojure.core.async :as a]
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

(defn- build-message-pipeline
  [socket pong-chan & handlers]
  (let [handler-tuples (partition-all 2 handlers)]
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
                  message))

           (map (fn [{message-type :type :as message}]
                  (when (= :pong message-type)
                    (a/go (a/>! pong-chan message)))
                  message))

           ;; Format message to tuple of [message [interested-handlers...]]
           (map (fn [message]
                  [message (->> handler-tuples
                                (filter (fn [[pred _]] (pred message)))
                                (map second))]))

           ;; Remove messages with no interested handlers
           (remove (comp empty? second))

           ;; Convert message to list of responses
           (map (fn [[message handlers :as m]]
                  (map #(% message) handlers))))))))

(defn- post-message!
  [socket {:keys [channel text]}]
  (ms/put! socket
           (json/write-str
            {:channel channel
             :id (get-message-id)
             :text text
             :type "message"})))

(defn- start-heartbeat
  [socket connected pong-chan]
  (while true
    (let [message-id (get-message-id)
          result @(ms/put! socket
                           (json/write-str
                            {:id message-id
                             :type "ping"}))]
      (when-not (let [[val _] (a/alts!! [pong-chan (a/timeout 5000)])]
                  (= {:type :pong :reply_to message-id} val))
        (prn "Ping response not received in time. Reconnecting...")
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
            pong-chan (a/chan 1)
            handler-stream (apply build-message-pipeline
                                  socket
                                  pong-chan
                                  handlers)
            connected (atom true)
            heartbeat (future
                        (Thread/sleep 3000)
                        (start-heartbeat socket connected pong-chan))]
        (try
          ;; Publish messages output by handler streams
          (while @connected
            (if-let [messages @(ms/take! handler-stream)]
              (doseq [message messages]
                (future (post-message! socket message)))))

          (finally (future-cancel heartbeat)
                   (ms/close! socket)))))))
