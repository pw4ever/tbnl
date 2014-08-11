(defproject info.voidstar/tbnl.cnc "0.1.0"
  :description "C&C controls figureheads through mastermind"
  :url "https://github.com/pw4ever/tbnl"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]

                 [info.voidstar/tbnl.core "0.1.0"]

                 [alembic "0.2.1"]
                 ;; graphviz support for visualization
                 [dorothy "0.0.5"]

                 ]
  :main ^:skip-aot cnc.main
  :target-path "target/%s"
  :profiles {
             :dev {
                   :plugins [
                             [lein-marginalia "0.7.1"]
                             ]
                   }             
             :uberjar {:aot :all}
             }
  :uberjar-name "tbnl.cnc-standalone.jar"
)
