(ns figurehead.plugin.command-executor.main
  (:require (figurehead.api.app [activity-manager :as activity-manager]))
  (:require (core [init :as init]
                  [state :as state]
                  [bus :as bus]
                  [plugin :as plugin]))
  (:require [clojure.core.async :as async :refer [chan close!
                                                  thread <!! >!!]]
            [clojure.java.io :as io]))

(def defaults
  (atom
   {
    :stop-unblock-tag :stop-figurehead.plugin.inject.intent
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
                                        param (:param content)]
                                    (case command
                                      :start-activity
                                      (do
                                        ;; inject intent
                                        (apply activity-manager/start-activity param))

                                      :start-service
                                      (do
                                        (apply activity-manager/start-service param))

                                      :force-stop
                                      (do
                                        (apply activity-manager/force-stop param))

                                      :kill
                                      (do
                                        (apply activity-manager/kill param))
                                      
                                      :kill-all
                                      (do
                                        (apply activity-manager/kill-all param))

                                      :else)))
                                
                                :else))
                            (recur (<!! ch))))))

(defn stop
  []
  (plugin/set-state-entry :figurehead.plugin.inject.intent
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
