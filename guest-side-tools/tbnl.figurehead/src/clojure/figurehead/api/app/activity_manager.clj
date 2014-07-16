(ns figurehead.api.app.activity-manager
  (:require (figurehead.util [services :as services :refer [get-service]])
            (figurehead.api.content [intent :as intent]))
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import (android.app IActivityManager)
           (android.content Intent
                            ComponentName)
           (android.net Uri)))

(declare start-activity start-service
         force-stop kill kill-all)

(defn start-activity
  "start an Activity

:wild-card - URI, component name, or package name
etc."
  [& param]
  (let [intent ^Intent (apply intent/make-intent param)
        param (into {} (map vec (partition 2 param)))
        activity-manager ^IActivityManager (get-service :activity-manager)
        mime-type (atom nil)
        wait? (:wait? param)]
    (when intent
      (reset! mime-type (.getType intent))
      (when (and (not @mime-type)
                 (.getData intent)
                 (= (.. intent getData getScheme) "content"))
        (reset! mime-type (.getProviderMimeType activity-manager
                                                (.getData intent)
                                                0)))
      (if wait?
        (.. activity-manager
            ^IactivityManager$WaitResult (startActivityAndWait nil nil ^Intent intent ^String @mime-type
                                                               nil nil 0 0
                                                               nil nil nil 0)
            result)
        (.. activity-manager
            (startActivityAsUser nil nil ^Intent intent ^String @mime-type
                                 nil nil 0 0
                                 nil nil nil 0))))))

(defn start-service
  "start Service"
  [& param]
  (let [intent ^Intent (apply intent/make-intent param)
        param (into {} (map vec (partition 2 param)))
        activity-manager ^IActivityManager (get-service :activity-manager)]
    (when intent
      (.. activity-manager
          ^ComponentName (startService nil intent (.getType intent) 0)))))

(defn force-stop
  "force stop a Package

:package - the package to force stop"
  [& param]
  (let [param (into {} (map vec (partition 2 param)))
        activity-manager ^IActivityManager (get-service :activity-manager)
        package (:package param)]
    (.forceStopPackage activity-manager package 0)))

(defn kill
  "kill a Package

:package - the package to kill"
  [& param]
  (let [param (into {} (map vec (partition 2 param)))
        activity-manager ^IActivityManager (get-service :activity-manager)
        package (:package param)]
    (.killBackgroundProcesses activity-manager package 0)))

(defn kill-all
  "kill all Packages"
  [& param]
  (let [activity-manager ^IActivityManager (get-service :activity-manager)]
    (.killAllBackgroundProcesses activity-manager)))


