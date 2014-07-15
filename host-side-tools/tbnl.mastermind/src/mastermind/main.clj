(ns mastermind.main
  (:require (core main
                  init))
  (:require core.plugin.echo.main
            core.plugin.nrepl.main
            mastermind.plugin.cnc.main
            mastermind.plugin.figurehead.main
            mastermind.plugin.model.figurehead.main)
  (:gen-class))

(defn -main
  "the main entry"
  [& args]

  (core.init/require-and-set-default-plugins core.plugin.echo
                                             core.plugin.nrepl
                                             mastermind.plugin.cnc
                                             mastermind.plugin.figurehead
                                             mastermind.plugin.model.figurehead)

  (apply core.main/main args))
