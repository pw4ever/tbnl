(ns figurehead.plugin.monitor.main
  (:require (figurehead.util [services :as services :refer [get-service]]))
  (:require (figurehead.api.app [activity-controller :as activity-controller]))
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
        now (Time.)
        activity-manager ^IActivityManager (get-service :activity-manager)

        activity-starting (fn [^Intent intent package]
                            (locking intent
                              (bus/say!! :activity-controller
                                         {:event :starting
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
                                          }
                                         verbose))
                            true)

        activity-resuming (fn [package]
                            (bus/say!! :activity-controller
                                       {:event :resuming
                                        :timestamp (do (.setToNow now)
                                                       (.toMillis now true))
                                        :package (-> package keyword)
                                        })
                            true)

        app-crashed (fn  [process-name pid
                          short-msg long-msg
                          time-millis stack-trace]
                      (doseq [^ActivityManager$RunningAppProcessInfo app-proc
                              (.getRunningAppProcesses activity-manager)]
                        (when (and (= pid (.pid app-proc))
                                   (= process-name (.processName app-proc)))
                          (bus/say!! :activity-controller
                                     {:event :crashed
                                      :timestamp (do (.setToNow now)
                                                     (.toMillis now true))
                                      :packages (into #{}
                                                      (map keyword
                                                           (.pkgList app-proc)))})))
                      true)

        app-early-not-responding (fn [process-name pid annotation]
                                   1)

        app-not-responding (fn [process-name pid process-stats]
                             1)

        system-not-responding (fn [msg]
                                1)]
    (plugin/blocking-jail [
                           ;; timeout
                           nil
                           ;; unblock-tag
                           (:stop-unblock-tag @defaults)
                           ;; finalization
                           (do
                             (activity-controller/set-activity-controller
                              {:reset? true}))
                           ;; verbose
                           verbose
                           ]
                          (activity-controller/set-activity-controller
                           {:activity-starting activity-starting
                            :activity-resuming activity-resuming
                            :app-crashed app-crashed
                            :app-early-not-responding app-early-not-responding
                            :app-not-responding app-not-responding
                            :system-not-responding system-not-responding}))))

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
