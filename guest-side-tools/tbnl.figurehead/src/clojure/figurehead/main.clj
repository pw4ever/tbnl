(ns figurehead.main
  "main entry into Figurehead"
  (:require (figurehead.util [init :as init]))
  (:require (core main
                  init))
  ;; these "require" are needed to handle lein-droid's :aot "removing unused namespace from classpath" feature
  (:require core.plugin.echo.main
            core.plugin.command-executor.main
            figurehead.plugin.nrepl.main
            figurehead.plugin.monitor.main
            figurehead.plugin.mastermind.main)
  (:gen-class))

(defn -main
  "the main entry"
  [& args]
  (init/init)
  (core.init/require-and-set-default-plugins core.plugin.echo
                                             core.plugin.command-executor
                                             figurehead.plugin.nrepl
                                             figurehead.plugin.monitor
                                             figurehead.plugin.mastermind)
  (apply core.main/main args))
