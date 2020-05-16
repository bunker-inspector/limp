(ns limp.client.rtm.handler
  (:require [clojure.spec.alpha :as s]
            [limp.client.rtm.events :as events])
  (:import com.slack.api.rtm.message.Message
           com.slack.api.rtm.RTMClient
           com.slack.api.rtm.RTMEventsDispatcherFactory
           com.slack.api.rtm.RTMEventsDispatcher
           com.slack.api.rtm.RTMEventsDispatcherImpl
           com.slack.api.rtm.RTMEventHandler
           handlerimpls.UserTypingEventHandler))

(s/fdef dispatcher
  :ret (partial instance? RTMEventsDispatcher))

(def dispatcher
  (memoize
   (fn [] (RTMEventsDispatcherFactory/getInstance))))

(s/fdef register!
  :args (s/cat :dispatcher (partial instance? RTMEventsDispatcher)
               :handler (partial instance? RTMEventHandler))
  :ret (partial instance? RTMEventsDispatcher))

(defn register!
  ([^RTMEventHandler handler]
   (.register (dispatcher) handler))
  ([^RTMEventsDispatcherImpl dispatcher
    ^RTMEventHandler handler]
   (.register dispatcher handler)
   dispatcher))

(s/fdef deregister!
  :args (s/cat :dispatcher (partial instance? RTMEventsDispatcher)
               :handler (partial instance? RTMEventHandler))
  :ret (partial instance? RTMEventsDispatcher))

(defn deregister!
  ([^RTMEventHandler handler]
   (.deregister (dispatcher) handler))
  ([^RTMEventsDispatcher dispatcher
    ^RTMEventHandler handler]
   (.deregister dispatcher handler)
   dispatcher))

(defmacro gen-handler
  [event-type responds? responder]
  `(fn [~'client]
     (~(symbol (str "limp.handlers." event-type "Handler."))
      ~responds? responder)))
