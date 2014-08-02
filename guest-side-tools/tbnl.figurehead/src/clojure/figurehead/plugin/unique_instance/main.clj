(ns figurehead.plugin.unique-instance.main
  "ensuring a unique instance of figurehead"
  (:require (core [init :as init]
                  [state :as state]
                  [bus :as bus]
                  [plugin :as plugin]))
  (:require (figurehead.api.os [util :as os-util]))
  (:import (android.os SystemProperties)))

(def defaults
  (atom
   {
    :sysprop-pid "figurehead.pid"
    :exit-code-on-existing 19
    }))

(defn populate-parse-opts-vector
  [current-parse-opts-vector]
  (init/add-to-parse-opts-vector [

                                  (let [option :kill]
                                    [nil
                                     (str "--"
                                          (name option)
                                          "")
                                     (str "kill existing instance and exit")])

                                  (let [option :replace]
                                    [nil
                                     (str "--"
                                          (name option)
                                          "")
                                     (str "replace existing instance and continue")])

                                  (let [option :is-running]
                                    [nil
                                     (str "--"
                                          (name option)
                                          "")
                                     (str "test whether an instance is already running")])

                                  ]))

(defn init
  [options]
  (let [kill? (:kill options)
        replace? (:replace options)
        is-running? (:is-running options)
        sysprop-pid (:sysprop-pid @defaults)
        cur-pid (os-util/get-my-pid {})
        sys-pid (os-util/get-system-property {:name sysprop-pid
                                              :def 0
                                              :int? true})]
    (when is-running?
      (println (if (os-util/test-process {:pid sys-pid})
                 "running"
                 "not running"))
      (System/exit 0))
    (state/add-state :sysprop-pid sysprop-pid)
    (.addShutdownHook ^Runtime (Runtime/getRuntime)
                      (Thread. #(when (= sys-pid cur-pid)
                                  (os-util/set-system-property {:name sysprop-pid
                                                                :value 0}))))
    (when (and (not= sys-pid 0)
               (os-util/test-process {:pid sys-pid}))
      ;; there is an existing instance
      (when (or replace? kill?)
        (os-util/kill-process {:pid sys-pid})
        (os-util/set-system-property {:name sysprop-pid
                                      :value 0}))
      (when-not replace?
        (System/exit (:exit-code-on-existing @defaults))))
    (state/add-state :cur-pid cur-pid)
    (os-util/set-system-property {:name sysprop-pid
                                  :value cur-pid})
    true))

(def config-map
  "the config map"
  {
   :populate-parse-opts-vector populate-parse-opts-vector
   :init init
   ;;:run run
   ;;:stop stop
   :param {:priority 1
           ;;:auto-restart true
           }})
