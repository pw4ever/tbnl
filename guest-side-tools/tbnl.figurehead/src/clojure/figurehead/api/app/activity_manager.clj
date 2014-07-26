(ns figurehead.api.app.activity-manager
  "am (Activity Manager) wrapper"
  (:require (core [state :as state :refer [defcommand]]))
  (:require (figurehead.util [services :as services :refer [get-service]])
            (figurehead.api.content [intent :as intent]))
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import (android.app IActivityManager
                        AppOpsManager)
           (android.content Intent
                            IIntentReceiver$Stub
                            ComponentName)
           (android.net Uri)
           (android.os Bundle
                       Binder)))

(declare start-activity start-service
         force-stop kill kill-all
         send-broadcast
         hang
         intent-to-uri)

(defcommand start-activity
  "start an Activity (accept all figurehead.api.content.intent/make-intent arguments)"
  [{:keys [wait?]
    :as args}]
  (let [intent ^Intent (intent/make-intent args)
        activity-manager ^IActivityManager (get-service :activity-manager)
        mime-type (atom nil)]
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

(defcommand start-service
  "start Service (accept all figurehead.api.content.intent/make-intent arguments)"
  [{:keys []
    :as args}]
  (let [intent ^Intent (intent/make-intent args)
        activity-manager ^IActivityManager (get-service :activity-manager)]
    (when intent
      (.. activity-manager
          ^ComponentName (startService nil intent (.getType intent) 0)))))

(defcommand force-stop
  "force stop a Package"
  [{:keys [package]
    :as args}]
  (let [activity-manager ^IActivityManager (get-service :activity-manager)]
    (.forceStopPackage activity-manager package 0)))

(defcommand kill
  "kill a Package"
  [{:keys [package]
    :as args}]
  (let [activity-manager ^IActivityManager (get-service :activity-manager)]
    (.killBackgroundProcesses activity-manager package 0)))

(defcommand kill-all
  "kill all Packages"
  [{:keys []
    :as args}]
  (let [activity-manager ^IActivityManager (get-service :activity-manager)]
    (.killAllBackgroundProcesses activity-manager)))


(defcommand send-broadcast
  "send broadcast"
  [{:keys [perform-receive
           receiver-permission]
    :as args}]
  (let [intent ^Intent (intent/make-intent args)

        activity-manager ^IActivityManager (get-service :activity-manager)

        intent-receiver (proxy
                            [IIntentReceiver$Stub]
                            []

                          (performReceive [^Intent intent result-code ^String data ^Bundle extras
                                           ordered sticky sending-user]
                            (when perform-receive
                              (perform-receive {:intent intent :result-code result-code
                                                :data data :extras extras
                                                :ordered ordered :sticky sticky
                                                :sending-user sending-user})))

                          )]
    (when (and intent intent-receiver)
      (.broadcastIntent activity-manager
                        nil intent nil intent-receiver
                        0 nil nil receiver-permission
                        AppOpsManager/OP_NONE true false 0))))

(defcommand hang
  "hang"
  [{:keys [allow-restart]
    :as args}]
  (let [activity-manager ^IActivityManager (get-service :activity-manager)]
    (.hang activity-manager (Binder.) allow-restart)))

(defcommand intent-to-uri
  "convert intent to URI"
  [{:keys [intent-scheme?]
    :as args}]
  (let [intent ^Intent (intent/make-intent args)]
    (when (and intent)
      (.toUri intent (if intent-scheme?
                       Intent/URI_INTENT_SCHEME
                       0)))))
