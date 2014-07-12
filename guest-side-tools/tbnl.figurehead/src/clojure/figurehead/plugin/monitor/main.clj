(ns figurehead.plugin.monitor.main
  (:require (figurehead.util [services :as services :refer [get-service]]))
  (:require (core [init :as init]
                  [state :as state]
                  [bus :as bus]
                  [plugin :as plugin]))
  (:require [clojure.string :as str]
            [clojure.core.async :as async])
  (:import
   (android.text.format Time)
   (android.app IActivityManager
                IActivityController$Stub
                ActivityManager$RunningAppProcessInfo)
   (android.content Intent)
   (android.content.pm IPackageManager
                       PackageManager
                       
                       ActivityInfo
                       ServiceInfo
                       ProviderInfo)))

(def defaults
  (atom
   {
    :stop-unblock-tag :stop-figurehead.plugin.monitor
    }))

(defn populate-parse-opts-vector
  [current-parse-opts-vector]
  (init/add-to-parse-opts-vector [
                                  
                                  ["-m"
                                   "--monitor"
                                   "enter monitor mode"]

                                  ]))

(defn init
  [options]
  (when (:monitor options)
    true))

(defn run
  [options]
  (let [verbose (:verbose options)
        instance-id (state/get-state :instance-id)
        now (Time.)
        activity-manager ^IActivityManager (get-service :activity-manager)
        ;; this is the meat
        activity-controller (proxy
                                [IActivityController$Stub]
                                []

                              (activityStarting [^Intent intent package]
                                (locking this
                                  (locking intent
                                    (bus/say!! :activity-controller.starting
                                               {:instance instance-id
                                                :timestamp (do (.setToNow now)
                                                               (.toMillis now true))
                                                :package (-> package keyword)
                                                :intent-action (-> intent .getAction keyword)
                                                ;; the "/" prevents straightforward keyword-ize
                                                :intent-component (str 
                                                                   (.. intent getComponent getPackageName)
                                                                   "/"
                                                                   (.. intent getComponent getShortClassName))
                                                :intent-category (into #{} (map keyword
                                                                                (.getCategories intent)))
                                                ;; data and extras may contain non-keyword-izable content
                                                :intent-data (-> intent .getDataString)
                                                :intent-extras (-> intent .getExtras)
                                                :intent-flags (-> intent .getFlags)
                                                }))
                                  true))

                              (activityResuming [package]
                                (locking this
                                  (bus/say!! :activity-controller.resuming
                                             {:instance instance-id
                                              :timestamp (do (.setToNow now)
                                                             (.toMillis now true))
                                              :package (-> package keyword)
                                              })
                                  true))

                              (appCrashed [process-name pid
                                           short-msg long-msg
                                           time-millis stack-trace]
                                (locking this
                                  (doseq [^ActivityManager$RunningAppProcessInfo app-proc (.getRunningAppProcesses activity-manager)]
                                    (when (and (= pid (.pid app-proc))
                                               (= process-name (.processName app-proc)))
                                      (bus/say!! :activity-controller.crashed
                                                 {:instance instance-id
                                                  :timestamp (do (.setToNow now)
                                                                 (.toMillis now true))
                                                  :packages (into #{}
                                                                  (map keyword
                                                                       (.pkgList app-proc)))})))
                                  true))

                              (appEarlyNotResponding [process-name pid annotation]
                                (locking this
                                  1))

                              (appNotResponding [process-name pid process-stats]
                                (locking this
                                  1))

                              (systemNotResponding [msg]
                                (locking this
                                  1))
                              
                              )]
    (plugin/blocking-jail [
                           ;; timeout
                           nil
                           ;; unblock-tag
                           (:stop-unblock-tag @defaults)
                           ;; finalization
                           (do
                             (.setActivityController activity-manager
                                                     nil))
                           ;; verbose
                           verbose
                           ]
                          (.setActivityController activity-manager
                                                  activity-controller))))

(defn stop
  [options]
  (plugin/set-state-entry :figurehead.plugin.monitor
                          :stop true)
  (plugin/unblock-thread (:stop-unblock-tag @defaults)))


(def config-map
  "the config map"
  {:populate-parse-opts-vector populate-parse-opts-vector
   :init init
   :run run
   :stop stop
   :param {:priority 1
           :auto-restart false}})
