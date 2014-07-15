(ns cnc.plugin.model.figurehead.visualize.main
  (:require (core [init :as init]
                  [state :as state]
                  [bus :as bus]
                  [plugin :as plugin]))
  (:require (cnc.plugin.model.figurehead.visualize [helper :as helper]))
  (:require [clojure.core.async :as async :refer [chan close!
                                                  go <! >! alts!]]
            [clojure.java.io :as io]))

(def defaults
  (atom
   {
    :stop-unblock-tag :stop-cnc.plugin.model.figurehead.visualization
    }))

(defn populate-parse-opts-vector
  [current-parse-opts-vector]
  (init/add-to-parse-opts-vector [
                                  
                                  [nil
                                   "--viz"
                                   "visualize figurehead model"]
                                  
                                  [nil
                                   "--viz-root [ROOT]"
                                   "visualization root file name"
                                   :default "viz"
                                   ]

                                  ]))

(defn init
  [options]
  (let [visualize (:visualize options)
        viz-root (:viz-root options)]
    (when (and viz-root)
      true)))

(defn run
  [options]
  (let [verbose (:verbose options)
        viz-root (:viz-root options)
        ch (chan)
        viz-counter (atom 0)]

    (plugin/blocking-jail [
                           ;; timeout
                           nil
                           ;; unblock-tag
                           (:stop-unblock-tag @defaults)
                           ;; finalization
                           (do
                             (bus/unsub-topic ch :model-update))
                           ;; verbose
                           verbose
                           ]
                          (bus/sub-topic ch :model-update)

                          ;; get the initial model
                          (bus/say!! :command {:topic :get-model :what {}})
                          
                          ;; listen for model update
                          (go
                            (loop [said (<! ch)]
                              (let [topic (bus/get-message-topic said)
                                    content (bus/remove-message-topic said)]
                                (case topic
                                  :model-update
                                  (do
                                    (let [type (:type content)
                                          model (:model content)]
                                      (case type
                                        :figurehead
                                        (do
                                          (swap! viz-counter inc)
                                          (helper/visualize model viz-root @viz-counter))

                                        :else)))
                                  
                                  :else))
                              (recur (<! ch)))))))

(defn stop
  []
  (plugin/set-state-entry :cnc.plugin.model.figurehead.visualization
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
