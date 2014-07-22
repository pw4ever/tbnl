(ns mastermind.plugin.figurehead.main
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
    :figurehead-port 4321
    :writer-buffer 10000
    }))

(defn populate-parse-opts-vector
  [current-parse-opts-vector]
  (init/add-to-parse-opts-vector [

                                  (let [option :figurehead-port
                                        default (option @defaults)]
                                    [nil
                                     (str "--"
                                          (name option)
                                          " [PORT]")
                                     (str "figurehead port")
                                     :default default
                                     :parse-fn (get-in @init/parse-opts-vector-helper
                                                       [:parse-fn :inet-port])])

                                  ]))

(defn init
  [options]
  (let [figurehead-port (:figurehead-port options)]
    (when (and figurehead-port)
      (plugin/set-state-entry :figurehead-port
                              figurehead-port)
      true)))

(defn run
  [options]
  (let [verbose (:verbose options)
        figurehead-port (:figurehead-port options)]
    (with-open [server-sock (ServerSocket. figurehead-port)]
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
                                               ;; cannot use say! -> deadlock
                                               (bus/say!! :information line verbose))
                                             (catch RuntimeException e
                                               ;; read-string error
                                               (when verbose
                                                 (prn [:figurehead :reader :RuntimeException])
                                                 (print-stack-trace e)))))
                                         (catch SocketException e
                                           ;; !! do not let IOException mask this
                                           (when verbose
                                             (prn [:figurehead :reader :SocketException])
                                             (print-stack-trace e))
                                           (throw e)))))
                                   (catch Exception e
                                     (when verbose
                                       (prn [:figurehead :reader :Exception])
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
                                         (bus/sub-topic ch :command)
                                         (while true
                                           (let [line (<! ch)]
                                             (.write writer (prn-str line))
                                             (.flush writer)))
                                         (finally
                                           (bus/unsub-topic ch :command)))))
                                   (catch Exception e
                                     (when verbose
                                       (prn [:figurehead :writer :Exception])
                                       (print-stack-trace e))
                                     (throw e))
                                   (finally
                                     (.close sock))))))))))

(defn stop
  []
  (plugin/set-state-entry :mastermind.plugin.figurehead
                          :stop true))

(def config-map
  "the config map"
  {
   :populate-parse-opts-vector populate-parse-opts-vector
   :init init
   :run run
   :stop stop
   :param {
           ;; connect to figureheads ASAP (but not as soon as session-id, etc.)
           :priority 90
           :auto-restart true}})
