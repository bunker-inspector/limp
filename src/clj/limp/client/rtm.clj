(ns limp.client.rtm
  (:require [clojure.spec.alpha :as s]
            [limp.client :as client])
  (:import com.slack.api.rtm.RTMClient))

(s/fdef register-handlers!
  :args (s/cat :client (partial instance? RTMClient)))

(s/fdef rtm
  :args (s/cat :token (s/? string?))
  :ret (partial instance? RTMClient))

(defn rtm
  ([]
   (rtm (System/getenv "SLACK_TOKEN")))
  ([token & handlers]
   (-> (client/slack)
       (.rtmConnect token))))
