(ns figurehead.util.unique-instance
  "work with a unique Figurehead instance"
  (:require (core [init :as init]
                  [state :as state]
                  [bus :as bus]
                  [plugin :as plugin]))
  (:require (figurehead.api.os [util :as os-util]))
  (:import (android.os SystemProperties)))

(declare
 ;; test whether running already
 is-running?
 ;; existing instance
 kill-existing-instance replace-existing-instance keep-existing-instance
 ;; pid
 get-pid set-pid
 ;; meta data
 meta-data
 get-meta-data-entry-sysprop
 get-meta-data-entry set-meta-data-entry unset-meta-data-entry
 get-meta-data unset-meta-data)

(def ^:private defaults
  (atom
   {
    :sysprop-pid "figurehead.pid"
    :sysprop-meta-data "figurehead.meta-data"
    :exit-code-on-existing 19
    }))

;;; test whether running already

(defn is-running?
  "test whether a Figurehead instance is already running"
  []
  (let [sys-pid (get-pid)]
    (os-util/test-process {:pid sys-pid})))

;;; existing instance

(defn kill-existing-instance
  "kill the existing Figurehead instance"
  []
  (let [sys-pid (get-pid)]
    (when (and (not= sys-pid 0)
               (is-running?))
      (os-util/kill-process {:pid sys-pid})
      (set-pid 0)
      (unset-meta-data))))

(let [run (fn []
            (let [cur-pid (os-util/get-my-pid {})] 
              (set-pid cur-pid)
              (.addShutdownHook ^Runtime (Runtime/getRuntime)
                                (Thread. #(let [sys-pid (get-pid)]
                                            (when (= sys-pid cur-pid)
                                              (set-pid 0)
                                              (unset-meta-data)))))))]

  (defn replace-existing-instance
    "replace the existing Figurehead instance with the current one"
    []
    (kill-existing-instance)
    (run))

  (defn keep-existing-instance
    "exit if there is an existing Figurehead instance, continue otherwise"
    []
    (if (is-running?)
      (System/exit (:exit-code-on-existing @defaults))
      (run))))

;;; pid

(defn get-pid
  "get pid of the unique instance"
  []
  (let [sysprop-pid (:sysprop-pid @defaults)
        pid (os-util/get-system-property {:name sysprop-pid
                                          :def 0
                                          :int? true})]
    pid))

(defn set-pid
  "set pid of the unique instance"
  [pid]
  (let [sysprop-pid (:sysprop-pid @defaults)]
    (os-util/set-system-property {:name sysprop-pid
                                  :value pid})))

;;; meta data

(def meta-data
  (atom {}))

(defn get-meta-data-entry-sysprop
  "get meta data entry's sysprop"
  [entry-name]
  (str "figurehead." (cond (keyword? entry-name)
                           (name entry-name)

                           :else
                           name)))

(defn get-meta-data-entry
  "get meta data entry"
  [name]
  (get @meta-data name))

(defn register-meta-data-entry
  "register meta data entry"
  [name]
  (swap! meta-data
         assoc name nil))

(defn set-meta-data-entry
  "set meta data entry"
  [name value]
  (let [sysprop (get-meta-data-entry-sysprop name)]
    (os-util/set-system-property {:name sysprop
                                  :value value})
    (swap! meta-data
           assoc name value)))

(defn unset-meta-data-entry
  "unset meta data entry"
  [name]
  (let [sysprop (get-meta-data-entry-sysprop name)]
    (swap! meta-data
           dissoc name)
    (os-util/set-system-property {:name sysprop
                                  :value ""})))

(defn get-meta-data
  "get all meta data of the unique instance"
  []
  (into {}
        (for [name (keys @meta-data)]
          (let [sysprop (get-meta-data-entry-sysprop name)
                entry (os-util/get-system-property {:name sysprop})]
            [name entry]))))

(defn unset-meta-data
  "unset all meta data of the unique instance"
  []
  (doseq [name (keys @meta-data)]
    (let [sysprop (get-meta-data-entry-sysprop name)]
      (os-util/set-system-property {:name sysprop
                                    :value ""}))))
