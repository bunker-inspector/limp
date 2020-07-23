# limp

A tiny library (if it can be called that) for making slackbots in Clojure

## Usage

A bot is created by calling the `start!` function where every 2 arguments is a predicate and a handler, resectively.
A predicate will return true if the following handler should respond to it. The handler should return 
a `Response` record.

Valid event types are the clj-ified versions of the ones listed [here](https://api.slack.com/rtm)

The `"SLACK_TOKEN"` environment variable should be set as your legacy bot token

Here's an example of a bot that will respond whenever anyone is typing (probably don't do this)
```clojure
(require '[limp.client.rtm :refer :all])

(start! (fn [{type :type}]
            (= :user-typing type))

        (fn [{channel :channel}]
            (Response. channel "someone is typing")))
```

## License

Copyright © 2020 Whoever

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
