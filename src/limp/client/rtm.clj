(ns limp.client.rtm
  (:require [clojure.spec.alpha :as s]
            [limp.client :as client]
            [limp.client.rtm.handler :as handler])
  (:import com.slack.api.rtm.RTMClient))

(s/fdef rtm
  :args (s/cat :token (s/? string?))
  :ret (partial instance? RTMClient))

(defn rtm
  ([]
   (rtm (System/getenv "SLACK_TOKEN")))
  ([token]
   (-> (client/slack)
       (.rtmConnect token))))

(s/fdef start!
  :args (s/cat :client (partial instance? RTMClient))
  :ret (partial instance? RTMClient))

(defn start!
  ([client]
   (start! client (handler/dispatcher)))
  ([client dispatcher]
   (->> dispatcher
        .toMessageHandler
        (.addMessageHandler client))
   (.connect client)
   client))
