(ns figurehead.ui.helper.figurehead
  "figurehead helpers"
  (:use (figurehead.ui su
                       util))
  (:require (neko [threading :refer [on-ui]]
                  [notify :refer [toast]]))
  (:require (clojure [string :as str]
                     [set :as set]
                     [pprint :refer [pprint]]
                     [stacktrace :refer [print-stack-trace]]))
  (:import (android.content Context))
  (:import (android.widget Switch
                           Button
                           CheckBox
                           EditText
                           TextView
                           ScrollView)
           (android.view View)
           (java.util List)))

(declare get-figurehead-apk-path build-figurehead-command
         figurehead-is-running? get-running-figurehead-state)

(defn get-figurehead-apk-path
  "get path to the backing APK"
  [^Context context]
  (let [apk-path (get-app-info-entry :apk-path)]
    ;; if already
    (if apk-path
      apk-path
      (when-let [package-manager (.getPackageManager context)]
        (let [package-name (.getPackageName context)]
          (when-let [app-info
                     (.getApplicationInfo package-manager
                                          package-name 0)]
            (let [apk-path (.publicSourceDir app-info)]
              (set-app-info-entry :apk-path apk-path)
              apk-path)))))))

(defn build-figurehead-command
  "build figurehead command to feed SU"
  [& commands]
  (when-let [figurehead-script @(get-app-info-entry :figurehead-script)]
    (str/join " " (into [figurehead-script] commands))))

;;; fast check of whether figurehead is running based on external commands

(def ^:private su-figurehead-is-running
  "the SU instance for figurehead-is-running?"
  (atom nil))

(defn figurehead-is-running?
  "return whether figurehead is running"
  []
  (let [is-running? (promise)
        commands ["pgrep -f figurehead.main"]
        timeout 60]
    (execute-root-command :commands commands
                          :timeout timeout
                          :callback? true
                          :buffered? true

                          :on-normal
                          (do
                            (try
                              (deliver is-running?
                                       (and output
                                            (not (empty? (.trim (str/join " " output))))))
                              (catch Exception e
                                (print-stack-trace e)
                                (deliver is-running? false))))

                          :on-error
                          (do
                            (deliver is-running?
                                     false))

                          :error-message
                          "Cannot determine whether Figurehead is running")
    @is-running?))

(defn get-running-figurehead-state
  "get the running figurehead session's state"
  []
  (let [state (promise)]
    (if (figurehead-is-running?)
      (do
        (let [commands [(build-figurehead-command "--status")]
              timeout 120]
          (execute-root-command :commands commands
                                :timeout timeout
                                :callback? true
                                :buffered? true

                                :on-normal
                                (do
                                  (let [output (str/join " " output)]
                                    (try
                                      (deliver state
                                               (read-string output))
                                      (catch Exception e
                                        (print-stack-trace e)
                                        (deliver state nil)))))

                                :on-error
                                (do
                                  (deliver state nil))

                                :error-message
                                "Cannot access Figurehead running state")))
      (do
        (deliver state nil)))
    @state))
