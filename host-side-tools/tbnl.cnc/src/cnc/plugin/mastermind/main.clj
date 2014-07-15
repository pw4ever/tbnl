(ns cnc.plugin.mastermind.main
  (:require (core [init :as init]
                  [state :as state]
                  [bus :as bus]
                  [plugin :as plugin]))
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.stacktrace :refer [print-stack-trace]]
            [clojure.core.async
             :as async
             :refer [chan thread <!! >!!]])
  (:import
   (java.net Socket)
   (java.io IOException)))

(def defaults
  (atom
   {
    :stop-unblock-tag :stop-cnc.plugin.mastermind
    :mastermind-port 1234
    :socket-timeout 15000
    :writer-buffer 1000
    :wait 3000
    }))

(defn populate-parse-opts-vector
  [current-parse-opts-vector]
  (init/add-to-parse-opts-vector [
                                  
                                  ["-a"
                                   "--mastermind-address ADDR"
                                   "mastermind address"]

                                  (let [option :mastermind-port
                                        default (option @defaults)]
                                    ["-p"
                                     (str "--"
                                          (name option)
                                          " [PORT]")
                                     (str "mastermind port")
                                     :default default
                                     :parse-fn (get-in @init/parse-opts-vector-helper
                                                       [:parse-fn :inet-port])])

                                  ]))

(defn init
  [options]
  (when (and (:mastermind-address options)
             (:mastermind-port options))
    true))

(defn run
  [options]
  (let [verbose (:verbose options)
        mastermind-address (:mastermind-address options)
        mastermind-port (:mastermind-port options)]
    (try
      (let [sock (Socket. ^String mastermind-address
                          ^int mastermind-port)]
        (plugin/blocking-jail [
                               ;; timeout
                               nil
                               ;; unblock-tag
                               (:stop-unblock-tag @defaults)
                               ;; finalization
                               (do
                                 (.close sock))
                               ;; verbose
                               verbose
                               ]

                              (.setSoTimeout sock (:socket-timeout @defaults))
                              ;; reader thread
                              (thread
                                (with-open [^java.io.BufferedReader reader (io/reader sock)]
                                  (plugin/looping-jail [
                                                        ;; stop condition
                                                        (plugin/get-state-entry :stop)
                                                        ;; finalization
                                                        (do
                                                          (.close sock))
                                                        ;; verbose
                                                        verbose
                                                        ]
                                                       (try
                                                         (when-let [line (.readLine reader)]
                                                           (try
                                                             (let [message (read-string line)
                                                                   topic (bus/get-message-topic message)
                                                                   content (bus/remove-message-topic message)]
                                                               (case topic
                                                                 :model-update
                                                                 (do
                                                                   (bus/say!! :model-update content))
                                                                 
                                                                 :information
                                                                 (do
                                                                   (bus/say!! :information content))))
                                                             (catch RuntimeException e
                                                               (when verbose
                                                                 (print-stack-trace e)))))
                                                         (catch IOException e
                                                           (when verbose
                                                             (print-stack-trace e)))))))
                              ;; writer thread
                              (thread
                                (with-open [^java.io.BufferedWriter writer (io/writer sock)]
                                  (let [ch (chan (:writer-buffer @defaults))]
                                    (bus/register-listener ch)
                                    (plugin/looping-jail [
                                                          ;; stop condition
                                                          (plugin/get-state-entry :stop)
                                                          ;; finalization
                                                          (do
                                                            (bus/unregister-listener ch)
                                                            (.close sock))
                                                          ;; verbose
                                                          verbose
                                                          ]
                                                         (let [message (<!! ch)
                                                               topic (bus/get-message-topic message)]
                                                           (cond
                                                            ;; do NOT echo these topics back
                                                            (not (contains? #{:information :model-update} topic))
                                                            (do
                                                              (.write writer (prn-str message))
                                                              (.flush writer))))))))))
      (catch java.net.UnknownHostException e
        (when verbose
          (prn [:java.net.UnknownHostException e])))
      (catch java.io.IOException e
        (when verbose
          (prn [:java.io.IOException e]))))))


(defn stop
  []
  (plugin/set-state-entry :cnc.plugin.mastermind
                          :stop true)
  (plugin/unblock-thread (:stop-unblock-tag @defaults)))


(def config-map
  "the config map"
  {:populate-parse-opts-vector populate-parse-opts-vector
   :init init
   :run run
   :stop stop
   :param {:priority 90
           :wait (:wait @defaults)
           :auto-restart true}})
