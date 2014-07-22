(ns mastermind.plugin.cnc.main
  (:require (core [init :as init]
                  [state :as state]
                  [bus :as bus]
                  [plugin :as plugin]))

  (:require [clojure.java.io :as io]
            [clojure.stacktrace :refer [print-stack-trace]]
            [clojure.core.async
             :as async
             :refer [chan
                     thread
                     go <! >!]]
   )
  (:import (java.net ServerSocket
                     SocketException)))

(def defaults
  (atom
   {
    :cnc-port 1234
    :writer-buffer 10000
    }))

(defn populate-parse-opts-vector
  [current-parse-opts-vector]
  (init/add-to-parse-opts-vector [

                                  (let [option :cnc-port
                                        default (option @defaults)]
                                    [nil
                                     (str "--"
                                          (name option)
                                          " [PORT]")
                                     (str "cnc port")
                                     :default default
                                     :parse-fn (get-in @init/parse-opts-vector-helper
                                                       [:parse-fn :inet-port])])

                                  ]))

(defn init
  [options]
  (let [cnc-port (:cnc-port options)]
    (when (and cnc-port)
      (plugin/set-state-entry :cnc-port
                              cnc-port)
      true)))

(defn run
  [options]
  (let [verbose (:verbose options)
        cnc-port (:cnc-port options)]
    (with-open [server-sock (ServerSocket. cnc-port)]
      (plugin/looping-jail [
                            ;; stop condition
                            (plugin/get-state-entry :stop)
                            ;; finalization
                            (do)
                            ;; verbose
                            verbose
                            ]
                           ;; .accept blocks until new connection
                           (let [sock (.accept server-sock)]
                             (thread
                               ;; reader
                               (go
                                 (try
                                   (with-open [reader (io/reader sock)]
                                     (while true
                                       (try
                                         (when-let [line (.readLine reader)]
                                           (try
                                             (let [line (read-string line)]
                                               ;; cannot use say! -> deadlock!!
                                               (bus/say!! :command line verbose))
                                             (catch RuntimeException e
                                               ;; read-string error
                                               (when verbose
                                                 (prn [:cnc :reader :RuntimeException])
                                                 (print-stack-trace e)))))
                                         (catch SocketException e
                                           (when verbose
                                             (prn [:cnc :reader :SocketException])
                                             (print-stack-trace e))
                                           (throw e)))))
                                   (catch Exception e
                                     (when verbose
                                       (prn [:cnc :reader :Exception])
                                       (print-stack-trace e))
                                     (throw e))
                                   (finally
                                     (.close sock))))
                               ;; writer
                               (go
                                 (try
                                   (with-open [writer (io/writer sock)]
                                     (let [ch (chan (:writer-buffer @defaults))]
                                       (try
                                         (bus/sub-topic ch :information)
                                         (bus/sub-topic ch :model-update)
                                         (while true
                                           (let [line (<! ch)]
                                             (.write writer (prn-str line))
                                             (.flush writer)))
                                         (finally
                                           (bus/unsub-topic ch :information)
                                           (bus/unsub-topic ch :model-update)))))
                                   (catch Exception e
                                     (when verbose
                                       (prn [:cnc :writer :Exception])
                                       (print-stack-trace e))
                                     (throw e))
                                   (finally
                                     (.close sock))))))))))

(defn stop
  []
  (plugin/set-state-entry :mastermind.plugin.cnc
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
