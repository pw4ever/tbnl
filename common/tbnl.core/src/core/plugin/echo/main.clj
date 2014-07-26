(ns core.plugin.echo.main
  "echo bus messages"
  (:require (core [init :as init]
                  [state :as state]
                  [bus :as bus]
                  [plugin :as plugin]))
  (:require [clojure.stacktrace :refer [print-stack-trace]]
            [clojure.core.async
             :as async
             :refer [chan
                     <!!
                     alt!!
                     timeout]]))

(def defaults
  (atom
   {
    :echo-buffer 100
    }))

(defn populate-parse-opts-vector
  [current-parse-opts-vector]
  (init/add-to-parse-opts-vector [
                                  
                                  ["-e"
                                   "--echo"
                                   "enable echo"]

                                  ]))

(defn init
  [options]
  (when (:echo options)
    true))

;;; archetype of looping-jail
(defn run
  [options]
  (let [verbose (:verbose options)
        ch (chan (:echo-buffer @defaults))]
    (bus/register-listener ch)
    (plugin/looping-jail [
                          ;; stop condition
                          (plugin/get-state-entry :stop)
                          ;; finalization
                          (do
                            (bus/unregister-listener ch))
                          ;; verbose
                          verbose]
                         (prn (<!! ch)))))

;;; archetype of stopping looping-jail
(defn stop
  [options]
  (plugin/set-state-entry :core.plugin.echo
                          :stop true))

(def config-map
  "the config map"
  {:populate-parse-opts-vector populate-parse-opts-vector
   :init init
   :run run
   :stop stop
   :param {:priority 100                ; echo needs to be run first in order to capture all transcripts
           :auto-restart false
           }})
