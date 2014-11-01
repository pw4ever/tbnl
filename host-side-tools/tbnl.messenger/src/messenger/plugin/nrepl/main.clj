(ns messenger.plugin.nrepl.main
  "connect to figurehead"
  (:require (core [init :as init]
                  [state :as state]
                  [bus :as bus]
                  [plugin :as plugin]))

  (:require [clojure.java.io :as io]
            [clojure.stacktrace :refer [print-stack-trace]]
            [clojure.pprint :refer [pprint]]
            [clojure.core.async
             :as async
             :refer [chan
                     thread
                     go <! >!]]
            [clojure.tools.nrepl :as repl]
   )
  (:import (java.net ServerSocket
                     SocketException)))

(def defaults
  (atom
   {
    :address "127.0.0.1"
    :port 12321
    }))

(defn populate-parse-opts-vector
  [current-parse-opts-vector]
  (init/add-to-parse-opts-vector [
                                  (let [option :address
                                        default (option @defaults)]
                                    ["-a"
                                     (str "--"
                                          (name option)
                                          " [ADDRESS]")
                                     (str "Figurehead host address")
                                     :default default
                                     ])


                                  (let [option :port
                                        default (option @defaults)]
                                    ["-p"
                                     (str "--"
                                          (name option)
                                          " [PORT]")
                                     (str "Figurehead nREPL port")
                                     :default default
                                     :parse-fn (get-in @init/parse-opts-vector-helper
                                                       [:parse-fn :inet-port])])
                                  ]))

(defn init
  [options]
  (let [address (:address options)
        port (:port options)]
    (when (and address port)
      (plugin/get-state-entry :address
                              address)
      (plugin/set-state-entry :port
                              port)
      true)))

(defn run
  [options]
  (let [verbose (:verbose options)
        address (:address options)
        port (:port options)]
    (plugin/looping-jail [
                          ;; stop condition
                          (plugin/get-state-entry :stop)
                          ;; finalization
                          (do)
                          ;; verbose
                          verbose
                          ]
                         ;; https://github.com/clojure/tools.nrepl#talking-to-an-nrepl-endpoint-programmatically
                         (with-open [conn (repl/connect :host address
                                                        :port port)]
                           (loop [sexp-str (pr-str (read *in*))]
                             (-> (repl/client conn 10000)
                                (repl/message {:op :eval
                                               :code sexp-str})
                                doall
                                pprint)
                             (recur (pr-str (read *in*))))))))

(defn stop
  []
  (plugin/set-state-entry :messenger.plugin.nrepl
                          :stop true))

(def config-map
  "the config map"
  {
   :populate-parse-opts-vector populate-parse-opts-vector
   :init init
   :run run
   :stop stop
   :param {
           ;; connect to cncs ASAP (but no as soon as session-id, etc.)
           :priority 90
           :auto-restart true
           }})
