(defproject limp "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [com.slack.api/slack-api-client "1.0.7"]
                 ;; Necessary for OpenJDK build
                 [javax.websocket/javax.websocket-api "1.1"]]
  :source-paths ["src/clj" "src/java"]
  :aot ["limp.client.rtm.handler.user-typing"]
  :java-source-paths ["src/java"]
  :repl-options {:init-ns limp.core}
  :javac-options ["-target" "1.7" "-source" "1.7" "-Xlint:-options"])
