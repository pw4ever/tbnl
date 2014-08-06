(ns figurehead.plugin.unique-instance.main
  "ensuring a unique instance of figurehead"
  (:require (core [init :as init]
                  [state :as state]
                  [bus :as bus]
                  [plugin :as plugin]))
  (:use (figurehead.util unique-instance))
  (:require (clojure [pprint :refer [pprint]]))
  (:import (android.os SystemProperties)))

(def defaults
  (atom
   {}))

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

                                  (let [option :status]
                                    [nil
                                     (str "--"
                                          (name option)
                                          "")
                                     (str "return status of existing instance and exit")])

                                  ]))

(defn init
  [options]
  true)

(defn run
  [options]
  (let [kill? (:kill options)
        replace? (:replace options)
        status? (:status options)]
    (cond status?
          (do
            (let [is-running? (is-running?)]
              (pprint (if is-running?
                        {:is-running? true
                         :state (get-meta-data)}
                        (do
                          (unset-meta-data)
                          {:is-running? false}))))
            (System/exit 0))

          kill?
          (do
            (kill-existing-instance)
            (System/exit 0)))

    (if replace?
      (replace-existing-instance)
      (keep-existing-instance))))

(def config-map
  "the config map"
  {
   :populate-parse-opts-vector populate-parse-opts-vector
   :init init
   :run run
   ;;:stop stop
   :param {:priority 1
           ;;:auto-restart true
           }})
