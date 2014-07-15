(defproject info.voidstar/tbnl.core "0.1.0-SNAPSHOT"
  :description "the infrastructure shared by mastermind and figurehead"
  :url "https://github.com/pw4ever/tbnl"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [org.clojure/tools.cli "0.3.1"]
                 [compliment "0.1.1"]]
  ;; prevent pom.xml marks nREPL with scope :test---so downstream will not include it in path
  ;; this makes use of core.plugin.nrepl fail miserably
  ;; 
  ;; references:
  ;; 
  ;; https://github.com/technomancy/leiningen/blob/2.4.2/src/leiningen/pom.clj#L328
  ;; https://maven.apache.org/pom.html#POM_Relationships
  ;; https://github.com/technomancy/leiningen/issues/1343
  ;; https://github.com/technomancy/leiningen/issues/1593
  ;;
  ;; temp fix: 
  ;; lein update-in :profiles:base empty -- update-in :profiles:base vec -- uberjar
  ;;
  ;:profiles {:base {:dependencies ^:replace []}}
  )
