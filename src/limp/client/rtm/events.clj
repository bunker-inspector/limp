(ns limp.client.rtm.events
  (:require [clojure.string :as str]))

(defprotocol ConvertsToEvent
  (->record [e] "Convert event from SDK source
                 object to clojure record implementing Event"))

(defmacro defevent
  [event-name & fields]
  (let [sdk-source-class (symbol (str "com.slack.api.model.event." event-name))
        simple-name (-> sdk-source-class str (str/split #"\.") last)
        record-cons-sym (symbol (str simple-name "."))
        type-hint-sym (symbol (str "^" (.getName sdk-source-class)))
        fields (sort fields)]
    `(do (defrecord ~event-name [~@fields])
         (extend-type ~(symbol sdk-source-class)
           ConvertsToEvent
           (~'->record [~'event]
            (~record-cons-sym ~@(map #(list (->> % name
                                                 str/capitalize
                                                 (str ".get")
                                                 symbol) 'event) fields)))))))

(defevent UserTypingEvent name channel)
