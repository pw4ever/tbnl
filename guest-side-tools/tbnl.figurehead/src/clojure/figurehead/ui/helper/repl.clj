(ns figurehead.ui.helper.repl
  "REPL helpers"
  (:use (figurehead.ui su
                       util))
  (:require (neko init
                  [threading :refer [on-ui]]
                  [notify :refer [toast]]))
  (:require (clojure [string :as str]
                     [set :as set]
                     [pprint :refer [pprint]]
                     [stacktrace :refer [print-stack-trace]]))
  (:require [clojure.core.async :as async])
  (:require [clojure.tools.nrepl.server :as nrepl-server]
            (cider.nrepl.middleware apropos
                                    classpath
                                    ;;complete
                                    ;;info
                                    inspect
                                    macroexpand
                                    resource
                                    stacktrace
                                    test
                                    ;;trace
                                    )))

(declare start-repl stop-repl)

(def repl-session
  "the sole REPL session to figurehead.ui"
  (atom nil))

(def ^:private cider-middleware
  "A vector containing all CIDER middleware."
  '[cider.nrepl.middleware.apropos/wrap-apropos
    cider.nrepl.middleware.classpath/wrap-classpath
    ;;cider.nrepl.middleware.complete/wrap-complete
    ;;cider.nrepl.middleware.info/wrap-info
    cider.nrepl.middleware.inspect/wrap-inspect
    cider.nrepl.middleware.macroexpand/wrap-macroexpand
    cider.nrepl.middleware.resource/wrap-resource
    cider.nrepl.middleware.stacktrace/wrap-stacktrace
    cider.nrepl.middleware.test/wrap-test
    ;;cider.nrepl.middleware.trace/wrap-trace
    ])

(def ^:private cider-nrepl-handler
  "CIDER's nREPL handler."
  (apply nrepl-server/default-handler (map resolve cider-middleware)))

(defn start-repl
  "start REPL is there is none"
  [& {:keys [port]
      :or {port 9999}
      :as args}]
  (when-not @repl-session
    (background-thread
     (reset! repl-session
             (neko.init/start-repl :port port
                                   :handler cider-nrepl-handler))
     ;; set up 'user ns
     (do
       (in-ns 'user)
       (use 'clojure.repl)
       (use 'clojure.pprint)
       (use 'clojure.java.io)
       (require 'clojure.set :as set)

       (use 'neko.doc)
       (use 'neko.debug)))))

(defn stop-repl
  "stop the sole"
  []
  (let [session @repl-session]
    (when session
      (background-thread
       (reset! repl-session nil)
       (nrepl-server/stop-server session)))))
