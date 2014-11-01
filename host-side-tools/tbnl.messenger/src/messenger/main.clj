(ns messenger.main
  "main entry into Messenger"
  (:require (core main
                  init))
  (:require core.plugin.echo.main
            ;;core.plugin.nrepl.main
            messenger.plugin.nrepl.main)
  (:gen-class))

(defn -main
  "the main entry"
  [& args]

  (core.init/require-and-set-default-plugins core.plugin.echo
                                             ;;core.plugin.nrepl
                                             messenger.plugin.nrepl)

  (apply core.main/main args))
