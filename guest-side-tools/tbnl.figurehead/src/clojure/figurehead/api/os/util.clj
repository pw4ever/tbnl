(ns figurehead.api.os.util
  "Android OS misc utils"
  (:require (core [state :as state :refer [defcommand]]))  
  (:require (figurehead.util [services :as services :refer [get-service]]))
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.set :as set :refer [subset?]])  

  (:import (android.os SystemProperties)))

(declare get-system-property set-system-property
         get-my-pid
         kill-process test-process)

(defcommand get-system-property
  "get system property by name"
  [{:keys [name
           def
           int?
           long?
           boolean?]
    :as args}]
  {:pre [name]}
  (when-let [name (str name)]
    (cond int?
          (SystemProperties/getInt name (if def def -1))

          long?
          (SystemProperties/getLong name (if def def -1))

          boolean?
          (SystemProperties/getBoolean name (if def def false))

          :else
          (SystemProperties/get name (if def def "")))))

(defcommand set-system-property
  "set system property by name to value"
  [{:keys [name
           value]
    :as args}]
  {:pre [name value]}
  (let [name (str name)
        value (str value)]
    (when (and name value)
      (SystemProperties/set name value))))

(defcommand get-my-pid
  "get my process id (pid)"
  [{:keys []
    :as args}]
  {:pre []}
  (android.os.Process/myPid))

(defcommand kill-process
  "kill process by pid"
  [{:keys [pid]
    :as args}]
  {:pre [pid]}
  (when pid
    (android.os.Process/killProcess pid)))

(defcommand test-process
  "test whether process is running by pid"
  [{:keys [pid]
    :as args}]
  {:pre [pid]}
  (when pid
    (let [ppid (android.os.Process/getParentPid pid)]
      (if (> ppid 0)
        true
        false))))
