(ns limp.client
  (:require [clojure.spec.alpha :as s])
  (:import com.slack.api.Slack
           com.slack.api.rtm.RTMClient
           com.slack.api.rtm.RTMEventsDispatcher))

(s/fdef slack
  :ret (partial instance? Slack))

(defn slack []
  (Slack/getInstance))
