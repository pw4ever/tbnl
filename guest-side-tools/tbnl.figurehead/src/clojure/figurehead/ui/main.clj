(ns figurehead.ui.main
  (:use (figurehead.ui su
                       util)
        (figurehead.ui.helper figurehead
                              widgets))
  (:require (neko [activity :refer [defactivity
                                    set-content-view!
                                    with-activity]]
                  [notify :refer [toast]]
                  [threading :refer [on-ui]]
                  [find-view :refer [find-view]]
                  log)
            (neko.listeners [view :refer [on-click]]))
  (:require (clojure [string :as str]
                     [pprint :refer [pprint]]))
  (:require [clojure.stacktrace :refer [print-stack-trace]])
  (:import (android.app Activity)
           (android.widget Switch
                           Button
                           CheckBox
                           EditText
                           TextView
                           ScrollView)
           (android.view View))
  (:import (java.util List))
  (:import (org.apache.commons.io FilenameUtils))
  (:import (figurehead.ui R$layout
                          R$id))
  (:import eu.chainfire.libsuperuser.Shell$Interactive))

(def widgets
  "all the widgets on this activity"
  (atom nil))

(defactivity figurehead.ui.main
  
  :on-create
  (fn [^Activity this bundle]

    (do
      ;; UI initialization

      (on-ui
       (set-content-view! this
                          R$layout/main))

      (with-activity this
        (reset! widgets
                {:figurehead-switch  ^Switch (find-view R$id/figurehead_switch)
                 :monitor ^CheckBox (find-view R$id/monitor)
                 :verbose ^CheckBox (find-view R$id/verbose)
                 :repl-port ^EditText (find-view R$id/repl_port)
                 :mastermind-address ^EditText (find-view R$id/mastermind_address)
                 :mastermind-port ^EditText (find-view R$id/mastermind_port)
                 :scroll-status ^ScrollView (find-view R$id/scroll_status)
                 :status ^TextView (find-view R$id/status)
                 :clear-status ^Button (find-view R$id/clear_status)})))

    (set-app-info-entry :figurehead-script (promise))

    (if (su?)
      (do
        (try
          (when-not (get-app-info-entry :apk-path)
            (set-app-info-entry :apk-path (get-figurehead-apk-path this)))
          (let [apk-path (get-app-info-entry :apk-path)]
            (if apk-path
              (do

                (let [path (FilenameUtils/getFullPath apk-path)
                      figurehead-script "/system/bin/figurehead"]

                  (let [commands [
                                  ;; create the script in one write
                                  (str "echo \""
                                       (str/join "\\n"
                                                 ["# bootstrapping Figurehead"
                                                  (str "export CLASSPATH=" apk-path)
                                                  (str "exec app_process "
                                                       path
                                                       " figurehead.main \\\"\\$@\\\"")])
                                       "\" > "
                                       figurehead-script)
                                  (str "chmod 700 "
                                       figurehead-script)]
                        ;; factor in the time it takes for user to authorize SU 
                        timeout 120]
                    (execute-root-command :commands commands
                                          :timeout timeout
                                          :callback? true
                                          :buffered? false

                                          :on-normal
                                          (do
                                            (deliver (get-app-info-entry :figurehead-script)
                                                     figurehead-script))

                                          :error-message
                                          (str/join " "
                                                    ["Cannot create"
                                                     figurehead-script])))))
              (do
                (on-ui
                 (toast "Figurehead cannot find its own APK."))
                (deliver (get-app-info-entry :figurehead-script)
                         nil))))
          (catch Exception e
            (print-stack-trace e))))
      (do
        ;; no SU
        (on-ui
         (toast "Superuser needed but not available!")))))

  :on-resume
  (fn [^Activity this]
    (let [widgets @widgets]
      (with-widgets widgets
        (sync-widgets-to-figurehead widgets)

        (on-ui
         (.setOnClickListener
          widget-clear-status
          (on-click
           ;; clear text
           (.setText widget-status "")))
         
         (.setOnCheckedChangeListener
          widget-figurehead-switch
          (proxy [android.widget.CompoundButton$OnCheckedChangeListener] []
            (onCheckedChanged [^android.widget.CompoundButton button-view
                               is-checked?]
              (background-looper-thread
               (let [figurehead-is-running? (figurehead-is-running?)]
                 (when (not= is-checked? figurehead-is-running?)
                   (on-ui
                    ;; temporarily disable widgets during state transition
                    (set-enabled widgets false)
                    ;; allow user to change her mind
                    (.setEnabled widget-figurehead-switch true))
                   (if is-checked?
                     (do
                       ;; turn on
                       (let [figurehead-args (into ["--replace"]
                                                   (widgets-to-figurehead-args widgets))
                             commands [(apply build-figurehead-command figurehead-args)]
                             ;; this is supposed to be a long running command
                             timeout 0]
                         (execute-root-command :commands commands
                                               :timeout timeout
                                               :callback? true
                                               :buffered? false

                                               :command-line-listener
                                               (do
                                                 (on-ui
                                                  (.append widget-status
                                                           (with-out-str (println line)))
                                                  (.post widget-scroll-status
                                                         #(.fullScroll widget-scroll-status
                                                                       View/FOCUS_DOWN))))

                                               :on-error
                                               (do
                                                 (.setEnabled widget-figurehead-switch true)
                                                 (.setEnabled widget-scroll-status true)
                                                 (.setEnabled widget-status true)
                                                 (.setEnabled widget-clear-status true))

                                               :error-message
                                               "Cannot start Figurehead"))
                       ;; enable needed widgets
                       (on-ui
                        (do
                          (.setEnabled widget-figurehead-switch true)
                          (.setEnabled widget-scroll-status true)
                          (.setEnabled widget-status true)
                          (.setEnabled widget-clear-status true)))) 
                     (do
                       ;; turn off
                       (let [commands [(build-figurehead-command "--kill")]
                             timeout 120]
                         (execute-root-command :commands commands
                                               :timeout timeout
                                               :callback? true
                                               :buffered? false

                                               :on-normal
                                               (do
                                                 (on-ui
                                                  (set-enabled widgets true)))

                                               :error-message
                                               "Cannot turn off Figurehead"))))))))))))))

  :on-pause
  (fn [^Activity this]
    (let [widgets @widgets]
      (with-widgets widgets
        (save-widget-state widgets))))

  :on-pause
  (fn [^Activity this]
    (let [widgets @widgets]
      (with-widgets widgets
        (save-widget-state widgets))))

  :on-destroy
  (fn [^Activity this]
    (let [widgets @widgets]
      (with-widgets widgets
        (save-widget-state widgets)))))
