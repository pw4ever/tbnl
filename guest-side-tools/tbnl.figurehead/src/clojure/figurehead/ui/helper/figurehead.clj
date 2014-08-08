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
         execute-root-command
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

(defmacro execute-root-command
  "execute root command with call back body"
  [{:keys [command
           buffered?
           threaded-callback?]
    :or {buffered? true
         threaded-callback? true}
    :as options}
   & callback-body]
  `(background-thread
    (try
      (let [su-instance# (open-root-shell)
            command# ~command]
        (try
          (when command#
            (apply send-root-command
                   :su-instance su-instance#
                   :commands command#
                   ~(if buffered?
                      `[:command-result-listener
                        (fn [~'command-code ~'exit-code ^List ~'output]
                          ~(if threaded-callback?
                             `(background-thread
                               ~@callback-body)
                             `(do
                                ~@callback-body)))]
                      `[:command-line-listener
                        (fn [^String ~'line]
                          ~(if threaded-callback?
                             `(background-thread
                               ~@callback-body)
                             `(do
                                ~@callback-body)))])))
          (catch Exception e#
            (print-stack-trace))
          (finally
            ;; clean-up one-shot SU resources
            (when su-instance#
              (.close su-instance#)))))
      (catch Exception e#
        (print-stack-trace e#)))))

;;; fast check of whether figurehead is running based on external commands

(def ^:private su-figurehead-is-running
  "the SU instance for figurehead-is-running?"
  (atom nil))

(defn figurehead-is-running?
  "return whether figurehead is running"
  []
  (let [is-running? (promise)
        command "pgrep -f figurehead.main"]
    (execute-root-command
     {:command command
      :buffered? true}
     (deliver is-running?
              (and output
                   (not (empty? (.trim (str/join " " output)))))))
    @is-running?))

(defn get-running-figurehead-state
  "get the running figurehead session's state"
  []
  (let [state (promise)]
    (if (figurehead-is-running?)
      (do
        (let [command (build-figurehead-command "--status")]
          (execute-root-command
           {:command command
            :buffered? true}
           (let [output (str/join " " output)]
             (try
               (deliver state
                        (read-string output))
               (catch Exception e
                 (print-stack-trace e)
                 (deliver state nil)))))))
      (do
        (deliver state nil)))
    @state))
