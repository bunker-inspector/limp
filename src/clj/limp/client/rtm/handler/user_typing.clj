(ns limp.client.rtm.handler.user-typing
  (:require [limp.client.rtm.events :as events])
  (:gen-class :extends handlerimpls.UserTypingEventHandler
              :prefix "-"
              :state state
              :init init
              :name limp.handlers.UserTypingEventHandler
              ))

(defn -init [responds? respond]
  [responds? respond])

(defn -handle [this message]
  (let [message (events/->record message)
        [responds? respond] (.state this)]
    (when (responds? message) (respond message))))
