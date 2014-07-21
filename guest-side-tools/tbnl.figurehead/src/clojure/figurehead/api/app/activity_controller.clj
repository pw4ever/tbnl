(ns figurehead.api.app.activity-controller
  (:require (figurehead.util [services :as services :refer [get-service]]))
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import (android.app IActivityManager
                        IActivityController$Stub)
           (android.content Intent)))

(declare set-activity-controller)

(defn set-activity-controller
  "set ActivityController"
  [{:keys [reset?

           activity-controller
           
           activity-starting
           activity-resuming
           app-crashed
           app-early-not-responding
           app-not-responding
           system-not-responding]
    :as args}]
  (let [activity-manager ^IActivityManager (get-service :activity-manager)
        activity-controller (cond reset? nil
                                  activity-controller activity-controller
                                  
                                  :else
                                  (proxy
                                      [IActivityController$Stub]
                                      []

                                    (activityStarting [^Intent intent package]
                                      (locking this
                                        (if activity-starting
                                          (activity-starting intent package)
                                          true)))

                                    (activityResuming [package]
                                      (locking this
                                        (if activity-resuming
                                          (activity-resuming package)
                                          true)))

                                    (appCrashed [process-name pid
                                                 short-msg long-msg
                                                 time-millis stack-trace]
                                      (locking this
                                        (if app-crashed
                                          (app-crashed process-name pid
                                                       short-msg long-msg
                                                       time-millis stack-trace)
                                          true)))

                                    (appEarlyNotResponding [process-name pid annotation]
                                      (locking this
                                        (if app-early-not-responding
                                          (app-early-not-responding process-name pid annotation)
                                          1)))

                                    (appNotResponding [process-name pid process-stats]
                                      (locking this
                                        (if app-not-responding
                                          (app-not-responding process-name pid process-stats)
                                          1)))

                                    (systemNotResponding [msg]
                                      (locking this
                                        (if system-not-responding
                                          (system-not-responding msg)
                                          1)))))]
    
    (.setActivityController activity-manager
                            activity-controller)))
