(ns cnc.main
  "main entry into C&C"
  (:require (core main
                  init))
  (:require core.plugin.echo.main
            core.plugin.nrepl.main
            cnc.plugin.mastermind.main
            cnc.plugin.model.figurehead.visualize.main
            )
  (:gen-class))

(defn -main
  "the main entry"
  [& args]

  (core.init/require-and-set-default-plugins core.plugin.echo
                                             core.plugin.nrepl
                                             cnc.plugin.mastermind
                                             cnc.plugin.model.figurehead.visualize
                                             )

  (apply core.main/main args))
