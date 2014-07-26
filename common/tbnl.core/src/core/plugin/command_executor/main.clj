(ns core.plugin.command-executor.main
  "listen for commands on bus and execute them"
  (:require (core [init :as init]
                  [state :as state]
                  [bus :as bus]
                  [plugin :as plugin]))
  (:require [clojure.core.async :as async
             :refer [chan <!!]]))

(def defaults
  (atom
   {
    :stop-unblock-tag :stop-core.plugin.command-executor
    }))

(defn populate-parse-opts-vector
  [current-parse-opts-vector]
  (init/add-to-parse-opts-vector [
                                  
                                  [nil
                                   "--no-command-executor"
                                   "disable command executor"]
                                  
                                  ]))

(defn init
  [options]
  (let [no-command-executor (:no-command-executor options)]
    (when-not no-command-executor
      true)))

(defn run
  [options]
  (let [verbose (:verbose options)
        ch (chan)]

    (plugin/blocking-jail [
                           ;; timeout
                           nil
                           ;; unblock-tag
                           (:stop-unblock-tag @defaults)
                           ;; finalization
                           (do
                             (bus/unsub-topic ch :command))
                           ;; verbose
                           verbose
                           ]
                          (bus/sub-topic ch :command)

                          ;; listen for model update
                          (loop [said (<!! ch)]
                            (let [topic (bus/get-message-topic said)
                                  content (bus/remove-message-topic said)]
                              (case topic
                                :command
                                (do
                                  (let [command (:command content)
                                        param (:param content)
                                        command-impl (state/get-command command)]
                                    (when command-impl
                                      (bus/say!! :response
                                                 {:command command
                                                  :result (command-impl param)}
                                                 verbose))))
                                :else))
                            (recur (<!! ch))))))

(defn stop
  []
  (plugin/set-state-entry :core.plugin.command-executor
                          :stop true)
  (plugin/unblock-thread (:stop-unblock-tag @defaults)))

(def config-map
  "the config map"
  {
   :populate-parse-opts-vector populate-parse-opts-vector
   :init init
   :run run
   :stop stop
   :param {
           :priority 1
           :auto-restart true
           }})
