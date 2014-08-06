(ns figurehead.plugin.nrepl.main
  "nREPL server with cider-nrepl middleware support"
  (:require (core [init :as init]
                  [state :as state]
                  [bus :as bus]
                  [plugin :as plugin]))
  (:require [figurehead.plugin.nrepl.helper :as helper])
  (:require (figurehead.util [unique-instance :refer [set-meta-data-entry
                                                      register-meta-data-entry]]))
  (:require [clojure.tools.nrepl.server :as nrepl-server]
            ;; comment out the middlewares that are incompatible with Dalvik
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
                                    )
            ;;[cider.nrepl :refer [cider-nrepl-handler]]
            compliment.core
            complete.core
            [clojure.stacktrace :refer [print-stack-trace]]
            [clojure.core.async
             :as async
             :refer [<!! chan]])
  (:import (java.util.concurrent TimeUnit
                                 ScheduledThreadPoolExecutor
                                 Executors
                                 ScheduledFuture)))

(def defaults
  (atom
   {
    :repl-clojure-cache-dir "figurehead-cache"
    :stop-unblock-tag :stop-figurehead.plugin.nrepl
    :clean-cache-interval (* 15 60)
    }))

(defn populate-parse-opts-vector
  [current-parse-opts-vector]
  (init/add-to-parse-opts-vector [

                                  (let [option :nrepl-port]
                                    ["-R"
                                     (str "--"
                                          (name option)
                                          " [PORT]")
                                     (str "nREPL port")
                                     :parse-fn (get-in @init/parse-opts-vector-helper
                                                       [:parse-fn :inet-port])])

                                  ]))

(defn init
  [options]
  (register-meta-data-entry :nrepl-port)
  (when (and (:nrepl-port options))
    true))

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

(defn run
  [options]
  (let [verbose (:verbose options)
        nrepl-port (:nrepl-port options)
        scheduler ^ScheduledThreadPoolExecutor (Executors/newScheduledThreadPool 1)
        clean-cache-task (delay ^ScheduledFuture
                                (.scheduleAtFixedRate scheduler
                                                      #(helper/clean-cache)
                                                      (:clean-cache-interval @defaults)
                                                      (:clean-cache-interval @defaults)
                                                      TimeUnit/SECONDS))]
    (set-meta-data-entry :nrepl-port nrepl-port)
    (plugin/blocking-jail [
                           ;; timeout
                           nil
                           ;; unblock-tag
                           (:stop-unblock-tag @defaults)
                           ;; finalization
                           (do
                             (nrepl-server/stop-server (plugin/get-state-entry :nrepl-server))
                             (.cancel ^ScheduledFuture @clean-cache-task true)
                             (helper/clean-cache))
                           ;; verbose
                           verbose
                           ]
                          (helper/enable-dynamic-compilation (:repl-clojure-cache-dir @defaults))
                          (when verbose
                            (prn [:repl-dynamic-compilation-path
                                  (plugin/get-state-entry :repl-dynamic-compilation-path)]))
                          (plugin/set-state-entry :nrepl-server
                                                  (helper/start-repl :port nrepl-port
                                                                     :handler cider-nrepl-handler))
                          
                          (plugin/register-exit-hook :figurehead.plugin.nrepl.clean-cache
                                                     #(helper/clean-cache))
                          ;; trigger the delayed task
                          @clean-cache-task)))

(defn stop
  [options]
  (plugin/set-state-entry :core.plugin.nrepl
                          :stop true)
  (plugin/unblock-thread (:stop-unblock-tag @defaults)))

(def config-map
  "the config map"
  {
   :populate-parse-opts-vector populate-parse-opts-vector
   :init init
   :run run
   :stop stop
   :param {:priority 1
           :auto-restart true}})
