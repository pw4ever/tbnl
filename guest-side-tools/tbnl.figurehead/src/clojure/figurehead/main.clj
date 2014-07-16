(ns figurehead.main
  (:require (figurehead.util [init :as init]))
  (:require (core main
                  init))
  ;; these "require" are needed to handle lein-droid's :aot "removing unused namespace from classpath" feature
  (:require core.plugin.echo.main
            figurehead.plugin.nrepl.main
            figurehead.plugin.monitor.main
            figurehead.plugin.getinfo.main
            figurehead.plugin.mastermind.main
            figurehead.plugin.command-executor.main)
  (:gen-class))

(defn -main
  "the main entry"
  [& args]
  (init/init)
  (core.init/require-and-set-default-plugins core.plugin.echo
                                             figurehead.plugin.nrepl
                                             figurehead.plugin.monitor
                                             figurehead.plugin.getinfo
                                             figurehead.plugin.mastermind
                                             figurehead.plugin.command-executor)
  (apply core.main/main args))
