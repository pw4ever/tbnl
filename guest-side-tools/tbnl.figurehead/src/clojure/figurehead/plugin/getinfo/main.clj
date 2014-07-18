(ns figurehead.plugin.getinfo.main
  (:require (figurehead.util [services :as services :refer [get-service]])
            (figurehead.api.content.pm [package-info :as package-info]))
  (:require (core [init :as init]
                  [state :as state]
                  [bus :as bus]
                  [plugin :as plugin]))
  (:require [clojure.string :as str]
            [clojure.core.async
             :as async
             :refer [<!! >!! timeout]])
  (:import
   (android.content.pm IPackageManager
                       PackageManager
                       
                       ActivityInfo
                       ServiceInfo
                       ProviderInfo
                       
                       PackageInfo)))

(def defaults
  (atom
   {
    :stop-unblock-tag :stop-figurehead.plugin.getinfo
    :repeat-interval 5000
    :wait 1000
    }))

(defn populate-parse-opts-vector
  [current-parse-opts-vector]
  (init/add-to-parse-opts-vector [
                                  
                                  ["-i"
                                   "--getinfo"
                                   "enter getinfo mode"]

                                  [nil
                                   "--package PACKAGE"
                                   "specify a target package"]

                                  ]))

(defn init
  [options]
  (when (:getinfo options)
    true))

(defn run
  [options]
  (let [verbose (:verbose options)
        package (:package options)
        instance-id (state/get-state :instance-id)]
    (plugin/blocking-jail [
                           ;; timeout
                           nil
                           ;; unblock-tag
                           (:stop-unblock-tag @defaults)
                           ;; finalization
                           (do)
                           ;; verbose
                           verbose
                           ]
                          (if package
                            (bus/say!! :package-manager.get-package-info 
                                       (assoc (package-info/get-package-info {:package package})
                                         :instance instance-id)
                                       verbose)
                            (bus/say!! :package-manager.get-all-packages
                                       (assoc {:packages (into #{}
                                                               (map (fn [^PackageInfo package-info]
                                                                      (-> package-info .packageName keyword))
                                                                    (package-info/get-all-packages {})))}
                                         :instance instance-id)
                                       verbose)))))

(defn stop
  [options]
  (plugin/set-state-entry :figurehead.plugin.getinfo
                          :stop true)
  (plugin/unblock-thread (:stop-unblock-tag @defaults)))

(def config-map
  "the config map"
  {:populate-parse-opts-vector populate-parse-opts-vector
   :init init
   :run run
   :stop stop
   :param {:priority 1
           :auto-restart false
           :wait (:wait @defaults)}})
