(ns core.plugin.nrepl.main
  (:require (core [init :as init]
                  [state :as state]
                  [bus :as bus]
                  [plugin :as plugin]))
  (:require [clojure.tools.nrepl.server :as nrepl-server]
            [clojure.stacktrace :refer [print-stack-trace]]
            [cider.nrepl :refer [cider-nrepl-handler]]
            [clojure.core.async
             :as async
             :refer [<!! chan]]))

(def defaults
  (atom
   {
    :stop-unblock-tag :stop-core.plugin.nrepl
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

;;; archetype of blocking-jail
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
                           (nrepl-server/stop-server (plugin/get-state-entry :nrepl-server))
                           ;; verbose
                           verbose
                           ]
                          (binding [*ns* (create-ns 'user)]
                            (refer-clojure)

                            (use 'clojure.repl)
                            (use 'clojure.pprint)
                            (use 'clojure.java.io)

                            (require '(core [bus :as bus]
                                            [plugin :as plugin]
                                            [state :as state]))
                            (require '[clojure.tools.nrepl.server :as nrepl-server])
                            
                            (plugin/set-state-entry :nrepl-server
                                                    ((resolve 'nrepl-server/start-server)
                                                     :port nrepl-port
                                                     :handler cider-nrepl-handler))))))

;;; archetype of stopping blocking-jail
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
   :param {:priority 0
           :auto-restart true}})
