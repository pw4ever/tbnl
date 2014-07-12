(defproject info.voidstar/tbnl.figurehead "0.0.1-SNAPSHOT"
  :description "figurehead sits on device and is controlled by mastermind"
  :url "https://github.com/pw4ever/tbnl"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"

  :global-vars {*warn-on-reflection* true}

  :source-paths ["src/clojure" "src"]
  :java-source-paths ["src/java" "gen"]
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]

  :dependencies [
                 [org.clojure-android/clojure "1.6.0-RC1" :use-resources true]
                 [org.clojars.pw4ever/neko-sdk18 "3.0.2"]
                 [info.voidstar/tbnl.core "0.1.0-SNAPSHOT"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [compliment "0.0.3"]
                 ]

  :profiles {:dev {
                   :android {
                             :aot :all-with-unused

                             :sdk-path "android-sdk"
                             }

                   }
             :release {
                       :android {;; Specify the path to your private keystore
                                 ;; and the the alias of the key you want to
                                 ;; sign APKs with. Do it either here or in
                                 ;; ~/.lein/profiles.clj
                                 ;; :keystore-path "/home/user/.android/private.keystore"
                                 ;; :key-alias "mykeyalias"

                                 :sdk-path "android-sdk"

                                 ;; for the default "debug.keystore"
                                 :keypass "android"
                                 :storepass "android"

                                 :ignore-log-priority [:debug :verbose]
                                 :aot :all}}
             }

  :aliases {
            "stage-debug" ["do" ["droid" "build"] ["droid" "apk"]]
            "stage-release" ["droid" "release" "build" "apk"]
            }

  :release-tasks [
                  ["droid" "release" "build" "apk"]
                  ]

  :android  {;; Specify the path to the Android SDK directory either
             ;; here or in your ~/.lein/profiles.clj file.
             
             :use-google-api true

             ;; Uncomment this if dexer fails with
             ;; OutOfMemoryException. Set the value according to your
             ;; available RAM.
             :dex-opts ["-JXmx4096M" "--num-threads=8"]

             ;; If previous option didn't work, uncomment this as well.
             :force-dex-optimize true

             :target-version "18"
             :aot-exclude-ns ["clojure.parallel" "clojure.core.reducers"
                              cljs.core.async.macros
                              cljs.core.async.impl.ioc-macros
                              ]})
