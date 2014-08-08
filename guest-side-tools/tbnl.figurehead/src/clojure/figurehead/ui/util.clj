(ns figurehead.ui.util
  "utilities"
  (:require (clojure [string :as str]
                     [set :as set])))

(declare
 ;; threading
 background-thread background-looper-thread
 ;; app-info
 app-info
 get-app-info-entry update-app-info-entry
 set-app-info-entry unset-app-info-entry)

;;; threading

(defmacro background-thread
  "run the body in a background thread"
  [& body]
  `(let [thread# (Thread.
                  (fn []
                    (android.os.Process/setThreadPriority
                     (android.os.Process/myTid)
                     android.os.Process/THREAD_PRIORITY_BACKGROUND)
                    ~@body))]
     (.start thread#)
     thread#))

(defmacro background-looper-thread
  "run the body in a background thread with a Looper"
  [& body]
  `(let [looper# (promise)
         thread# (Thread.
                  (fn []
                    (android.os.Looper/prepare)
                    (deliver looper# (android.os.Looper/myLooper))
                    (android.os.Process/setThreadPriority
                     (android.os.Process/myTid)
                     android.os.Process/THREAD_PRIORITY_BACKGROUND)
                    ~@body
                    (android.os.Looper/loop)))]
     (.start thread#)
     looper#))

;;; app info

(def app-info
  "app info"
  (atom {}))

(defn get-app-info-entry
  "get app info entry with <name>"
  [name]
  (get @app-info name))

(defn update-app-info-entry
  "update app info entry with <name> to (apply f <name> args)"
  [name f & args]
  (apply swap! app-info
         update-in [name] f args))

(defn set-app-info-entry
  "set app info entry with <name> to <value>"
  [name value]
  (swap! app-info
         assoc name value))

(defn unset-app-info-entry
  "unset app info entry with <name>"
  [name]
  (swap! app-info
         dissoc name))
