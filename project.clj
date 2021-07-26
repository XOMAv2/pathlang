(defproject pathlang "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.879"]
                 [cljc.java-time "0.1.16"]
                 [cljsjs/js-joda-timezone "2.2.0-0"]
                 [expound "0.8.9"] ; Приятные отчёты об ошибках для clojure.spec.alpha.
                 [org.clojure/core.match "1.0.0"]
                 ]
  :repl-options {:init-ns pathlang.interpreter})
