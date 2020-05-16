(ns limp.client.rtm
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [gniazdo.core :as ws]
            [clojure.core.async
             :refer [>! <! >!! <!! go chan buffer close! thread
                     alts! alts!! timeout go-loop]]))

(def session-message-id (atom 0))
(def get-message-id (partial swap! session-message-id inc))

(defrecord Response
    [channel text])

(s/fdef client
:args (s/cat :token (s/? string?)))

(defn start!
  [& handlers]
  (let [token
        (System/getenv "SLACK_TOKEN")

        {:keys [body]}
        (http/get "https://slack.com/api/rtm.connect"
                  {:query-params {"token" token}})

        {:keys [ok url] :as body-parsed
         {:keys [self-id]} :self}
        (json/read-str body :key-fn keyword)

        reply-chan
        (chan)]
    (if ok
      (let [conn
            (ws/connect url
              :on-close (fn [error-code error-reason]
                          (println error-code)
                          (println error-reason))
              :on-receive (fn [event]
                            (let [event (json/read-str event
                                                       :key-fn keyword)]
                              (clojure.pprint/pprint event)
                              (doseq [[pred response-fn] (partition-all 2 handlers)]
                                (try
                                  (when (pred event)
                                    (go (>!! reply-chan (response-fn event))))
                                  (catch Exception e
                                    (prn (format "Caught exception in message handler: %s"
                                                 (.getMessage e)))))))))]
        (go-loop []
          (let [{:keys [channel text]} (<! reply-chan)]
            (ws/send-msg conn (json/write-str {:id (get-message-id)
                                               :type "message"
                                               :channel channel
                                               :text text})))
          (recur))
        conn)
      (prn "Failed to authenticate..."))))

(comment
  (def foo (start! (fn [{:keys [text] :as event}]
                     (when text
                       (str/includes? text "potato")))

                   (fn [{:keys [channel]}]
                     (Response. channel "is someone talking about potatoes?")))))
