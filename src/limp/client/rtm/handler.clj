(ns limp.client.rtm.handler
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [limp.client.rtm.events :as events])
  (:import com.slack.api.rtm.message.Message
           (com.slack.api.rtm RTMClient
                              RTMEventsDispatcherFactory
                              RTMEventsDispatcher
                              RTMEventsDispatcherImpl
                              RTMEventHandler)
           (limp.client.rtm.events UserTypingEvent)))

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

(defn handler
  [event-type responds? responder]
  (let [unqualified-name (as-> event-type %
                           (.getName %)
                           (str/split % #"\.")
                           (last %))
        sdk-class-name (str "com.slack.api.model.event."
                            unqualified-name)]
    (proxy [RTMEventHandler] []
      (handle [event]
        (let [event (events/->record event)]
          (when (responds? event)
            (responder event))))
      (getEventClass [] (Class/forName sdk-class-name))
      (getEventType []
        (print "HERE")
        sdk-class-name))))
