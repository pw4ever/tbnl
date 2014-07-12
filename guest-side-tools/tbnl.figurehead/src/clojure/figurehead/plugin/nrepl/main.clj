(ns figurehead.plugin.nrepl.main
  (:require (core [init :as init]
                  [state :as state]
                  [bus :as bus]
                  [plugin :as plugin]))
  (:require [figurehead.plugin.nrepl.helper :as helper])
  (:require [clojure.tools.nrepl.server :as nrepl-server]
            [clojure.stacktrace :refer [print-stack-trace]]
            [clojure.core.async
             :as async
             :refer [<!! chan]]))

(def defaults
  (atom
   {
    :repl-clojure-cache-dir "clojure-cache"
    :repl-worker-thread-stack-size 1048576     ; nrepl 1 M
    :stop-unblock-tag :stop-figurehead.plugin.nrepl
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
  (when (:nrepl-port options)
    true))

(defn run
  [options]
  (let [verbose (:verbose options)
        nrepl-port (:nrepl-port options)]
    (plugin/blocking-jail [
                           ;; timeout
                           nil
                           ;; unblock-tag
                           (:stop-unblock-tag @defaults)
                           ;; finalization
                           (do
                             (nrepl-server/stop-server (plugin/get-state-entry :nrepl-server))
                             ;; TODO: clear (plugin/get-state-entry :repl-dynamic-compilation-path)  ;; could be dangerous
                             )
                           ;; verbose
                           verbose
                           ]
                          ;; refer to neko.init/init and neko.compilation/init; replace all use of Context with regular file operation
                          (helper/enable-dynamic-compilation (:repl-clojure-cache-dir @defaults))
                          (when verbose
                            (prn [:repl-dynamic-compilation-path
                                  (plugin/get-state-entry :repl-dynamic-compilation-path)]))
                          (plugin/set-state-entry :nrepl-server
                                                  (helper/start-repl :port nrepl-port)))))

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
