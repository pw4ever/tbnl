(ns figurehead.plugin.command-executor.main
  (:require (figurehead.api.app [activity-manager :as activity-manager])
            (figurehead.api.content.pm [package-manager :as package-manager]
                                       [package-manager-parser :as package-manager-parser])
            (figurehead.api.content [intent :as intent])
            (figurehead.api.view [input :as input]))
  (:require (core [init :as init]
                  [state :as state]
                  [bus :as bus]
                  [plugin :as plugin]))
  (:require [clojure.core.async :as async :refer [chan close!
                                                  thread <!! >!!]]
            [clojure.java.io :as io])
  (:import (android.content.pm PackageInfo)))

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
                                      
                                      :get-raw-packages
                                      (do
                                        (bus/say!! :response
                                                   {:command :get-raw-packages
                                                    :result (package-manager/get-raw-packages param)}
                                                   verbose))

                                      :get-packages
                                      (do
                                        (bus/say!! :response
                                                   {:command :get-packages
                                                    :result (package-manager/get-packages param)}
                                                   verbose))

                                      :get-all-package-names
                                      (do
                                        (bus/say!! :response
                                                   {:command :get-all-package-names
                                                    :result (package-manager/get-all-package-names param)}
                                                   verbose))

                                      :get-package-components
                                      (do
                                        (bus/say!! :response
                                                   {:command :get-package-components
                                                    :result (package-manager/get-package-components param)}
                                                   verbose))                                  
                                      
                                      :get-raw-features
                                      (do
                                        (bus/say!! :response
                                                   {:command :get-raw-features
                                                    :result (package-manager/get-raw-features param)}
                                                   verbose))

                                      :get-features
                                      (do
                                        (bus/say!! :response
                                                   {:command :get-features
                                                    :result (package-manager/get-features param)}
                                                   verbose))

                                      :get-raw-libraries
                                      (do
                                        (bus/say!! :response
                                                   {:command :get-raw-libraries
                                                    :result (package-manager/get-raw-libraries param)}
                                                   verbose))

                                      :get-libraries
                                      (do
                                        (bus/say!! :response
                                                   {:command :get-libraries
                                                    :result (package-manager/get-libraries param)}
                                                   verbose))

                                      :get-raw-instrumentations
                                      (do
                                        (bus/say!! :response
                                                   {:command :get-raw-instrumentations
                                                    :result (package-manager/get-raw-instrumentations param)}
                                                   verbose))

                                      :get-instrumentations
                                      (do
                                        (bus/say!! :response
                                                   {:command :get-instrumentations
                                                    :result (package-manager/get-instrumentations param)}
                                                   verbose))                                      

                                      :get-raw-permission-groups
                                      (do
                                        (bus/say!! :response
                                                   {:command :get-raw-permission-groups
                                                    :result (package-manager/get-raw-permission-groups param)}
                                                   verbose))

                                      :get-permissions-by-group
                                      (do
                                        (bus/say!! :response
                                                   {:command :get-permissions-by-group
                                                    :result (package-manager/get-permissions-by-group param)}
                                                   verbose))

                                      :make-intent
                                      (do
                                        (intent/make-intent param))
                                      
                                      :start-activity
                                      (do
                                        (activity-manager/start-activity param))

                                      :start-service
                                      (do
                                        (activity-manager/start-service param))

                                      :force-stop
                                      (do
                                        (activity-manager/force-stop param))

                                      :kill
                                      (do
                                        (activity-manager/kill param))
                                      
                                      :kill-all
                                      (do
                                        (activity-manager/kill-all param))

                                      :send-broadcast
                                      (do
                                        (activity-manager/send-broadcast param))

                                      :hang
                                      (do
                                        (activity-manager/hang param))

                                      :intent-to-uri
                                      (do
                                        (bus/say!! :response
                                                   {:command :intent-to-uri
                                                    :result (activity-manager/intent-to-uri param)}
                                                   verbose))

                                      :text
                                      (do
                                        (input/text param))

                                      :key-event
                                      (do
                                        (input/key-event param))

                                      :tap
                                      (do
                                        (input/tap param))

                                      :swipe
                                      (do
                                        (input/swipe param))

                                      :touchscreen
                                      (do
                                        (input/touchscreen param))

                                      :touchpad
                                      (do
                                        (input/touchpad param))

                                      :touch-navigation
                                      (do
                                        (input/touch-navigation param))

                                      :trackball
                                      (do
                                        (input/trackball param))

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
