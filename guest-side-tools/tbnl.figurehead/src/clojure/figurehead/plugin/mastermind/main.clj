(ns figurehead.plugin.mastermind.main
  "connect to Mastermind"
  (:require (core [init :as init]
                  [state :as state]
                  [bus :as bus]
                  [plugin :as plugin]))
  (:require (figurehead.util [unique-instance :refer [set-meta-data-entry
                                                      register-meta-data-entry]]))
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.stacktrace :refer [print-stack-trace]]
            [clojure.pprint :refer [pprint]]
            [clojure.core.async
             :as async
             :refer [thread chan <!! >!!]])
  (:import
   (java.net Socket
             SocketTimeoutException)))

(def defaults
  (atom
   {
    :stop-unblock-tag :stop-figurehead.plugin.mastermind
    :mastermind-port 4321
    :socket-timeout 15000
    :writer-buffer 1000
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
  (register-meta-data-entry :mastermind-address)
  (register-meta-data-entry :mastermind-port)
  (when (and (:mastermind-address options)
             (:mastermind-port options))
    true))

(defn run
  [options]
  (let [verbose (:verbose options)
        mastermind-address (:mastermind-address options)
        mastermind-port (:mastermind-port options)
        instance-id (state/get-state :instance-id)]
    (set-meta-data-entry :mastermind-address mastermind-address)
    (set-meta-data-entry :mastermind-port mastermind-port)
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
                                                             (when verbose
                                                               (pprint [:mastermind :reader message]))
                                                             (case topic
                                                               :command
                                                               (do
                                                                 (bus/say!! :command content))

                                                               :else))
                                                           (catch RuntimeException e
                                                             (when verbose
                                                               (print-stack-trace e)))))
                                                       (catch SocketTimeoutException e
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
                                                             topic (bus/get-message-topic message)
                                                             content (bus/remove-message-topic message)]
                                                         (cond
                                                          ;; do NOT echo these topics back
                                                          (not (contains? #{:command} topic))
                                                          (let [message (bus/build-message topic
                                                                                           (cond
                                                                                            (map? content)
                                                                                            (merge content
                                                                                                   {:instance instance-id})

                                                                                            :else
                                                                                            {:instance instance-id
                                                                                             :content message}))]
                                                            (when verbose
                                                              (pprint [:mastermind :writer message]))
                                                            (.write writer
                                                                    (prn-str message))
                                                            (.flush writer))))))))))))


(defn stop
  []
  (plugin/set-state-entry :figurehead.plugin.mastermind
                          :stop true)
  (plugin/unblock-thread (:stop-unblock-tag @defaults)))


(def config-map
  "the config map"
  {:populate-parse-opts-vector populate-parse-opts-vector
   :init init
   :run run
   :stop stop
   :param {:priority 90
           :auto-restart true}})
