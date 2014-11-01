(defproject info.voidstar/tbnl.messenger "0.1.1-SNAPSHOT"
  :description "messenger sends nREPL commands to figureheads"
  :url "https://github.com/pw4ever/tbnl"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [
                 [org.clojure/clojure "1.6.0"]

                 [info.voidstar/tbnl.core "0.1.1-SNAPSHOT"]

                 ]
  :main ^:skip-aot messenger.main
  :target-path "target/%s"
  :profiles {
             :dev {
                   :plugins [
                             [lein-marginalia "0.7.1"]
                             ]
                   }
             :uberjar {:aot :all}
             }
  :uberjar-name "tbnl.messenger-standalone.jar"
)
