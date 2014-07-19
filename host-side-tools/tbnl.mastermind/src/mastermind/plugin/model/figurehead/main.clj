(ns mastermind.plugin.model.figurehead.main
  (:require (core [init :as init]
                  [state :as state]
                  [bus :as bus]
                  [plugin :as plugin]))
  (:require (mastermind.plugin.model.figurehead [helper :as helper]
                                                [model :as model]))
  (:require [clojure.core.async :as async :refer [chan close!
                                                  go <! >! alts!]]
            [clojure.java.io :as io]))

(def defaults
  (atom
   {
    :stop-unblock-tag :stop-mastermind.plugin.model.figurehead
    :monitor-trace []
    }))

(defn populate-parse-opts-vector
  [current-parse-opts-vector]
  (init/add-to-parse-opts-vector [
                                  
                                  [nil
                                   "--model-figurehead"
                                   "model figurehead traces"
                                   :default true
                                   ]

                                  (let [option :monitor-trace
                                        default (option @defaults)]
                                    [nil
                                     (str "--"
                                          (name option)
                                          " [TRACE]")
                                     (str "[m] figurehead trace")
                                     :default default
                                     :parse-fn (get-in @init/parse-opts-vector-helper
                                                       [:parse-fn :file])
                                     :assoc-fn (fn [m k v]
                                                 (update-in m [k]
                                                            (comp vec conj) v))])

                                  [nil
                                   "--monitor-trace-output [OUTPUT]"
                                   "figurehead traces transcript output"
                                   :parse-fn (get-in @init/parse-opts-vector-helper
                                                     [:parse-fn :file])]

                                  ]))

(defn init
  [options]
  (let [model-figurehead (:model-figurehead options)]
    (when (and model-figurehead)
      true)))

(defn run
  [options]
  (let [verbose (:verbose options)
        monitor-trace (:monitor-trace options)
        monitor-trace-output (:monitor-trace-output options)
        monitor-trace-output-writer (when monitor-trace-output
                                      (io/writer monitor-trace-output))
        dump-monitor-trace (fn [content]
                             (when monitor-trace-output-writer
                               (binding [*out* monitor-trace-output-writer]
                                 (prn content)
                                 (flush))))
        model (model/init-model)
        state (model/init-state)
        input-chans (helper/create-input-chans monitor-trace)
        ch (chan)]

    (plugin/blocking-jail [
                           ;; timeout
                           nil
                           ;; unblock-tag
                           (:stop-unblock-tag @defaults)
                           ;; finalization
                           (do
                             (when monitor-trace-output-writer
                               (.close monitor-trace-output-writer))
                             (bus/unsub-topic ch :command)
                             (bus/unsub-topic ch :information)
                             (doseq [input-chan @input-chans]
                               (async/close! input-chan)))
                           ;; verbose
                           verbose
                           ]
                          (bus/sub-topic ch :command)
                          (bus/sub-topic ch :information)
                          
                          (let [model-ch (chan)]
                            ;; listen and respond to information and command
                            (go
                              (loop [said (<! ch)]
                                (let [topic (bus/get-message-topic said)
                                      content (bus/remove-message-topic said)]
                                  (case topic
                                    :command
                                    (do
                                      (let [topic (bus/get-message-topic content)
                                            content (bus/remove-message-topic content)]
                                        (case topic
                                          :get-model
                                          (do
                                            (model/broadcast-model @model))

                                          :else)))
                                    
                                    
                                    :information
                                    (do
                                      (let [topic (bus/get-message-topic content)
                                            content (bus/remove-message-topic content)]
                                        (case topic
                                          :activity-controller
                                          (do
                                            (>! model-ch content))

                                          :else)))

                                    :else))
                                (recur (<! ch))))
                            ;; update model
                            (go
                              (loop [input @input-chans]
                                (let [alt-chs (into [model-ch] (vals input))
                                      [news _] (alts! alt-chs)]
                                  (dump-monitor-trace news)
                                  (when (model/update-model! model state news)
                                    (model/broadcast-model @model)))
                                (recur @input-chans)))))))

(defn stop
  []
  (plugin/set-state-entry :mastermind.plugin.model.figurehead
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
           ;; connect to cncs ASAP (but no as soon as session-id, etc.)
           :priority 95
           :auto-restart true
           }})
