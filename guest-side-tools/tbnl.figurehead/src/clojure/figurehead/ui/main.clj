(ns figurehead.ui.main
  (:use (figurehead.ui su
                       helper
                       util))
  (:require (neko [activity :refer [defactivity
                                    set-content-view!
                                    with-activity]]
                  [notify :refer [toast]]
                  [ui :refer [make-ui]]
                  [threading :refer [on-ui]]
                  [context :refer [get-service inflate-layout]]
                  data
                  doc
                  debug
                  [find-view :refer [find-view]]
                  log)
            (neko.listeners adapter-view
                            dialog
                            search-view
                            text-view
                            [view :refer [on-click]])
            (neko.ui adapters
                     listview
                     mapping
                     menu
                     [traits :as traits]))
  (:require (clojure [string :as str]
                     [pprint :refer [pprint]]))
  (:require [clojure.core.async :refer [thread]])
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
                          R$id)))

(defactivity figurehead.ui.main
  
  :on-create
  (fn [^Activity this bundle]

    (set-app-info-entry :figurehead-script (promise))

    (background-thread
     (if (su?)
       (do
         (set-app-info-entry :su? true)
         (try
           ;; apk-path
           (when-let [apk-path (get-figurehead-apk-path this)]
             (set-app-info-entry :apk-path apk-path))

           ;; create and wait for root shell
           (let [su-instance (open-root-shell)]

             (when-let [apk-path (get-app-info-entry :apk-path)]
               ;; create the figurehead script
               (let [figurehead-script "/system/bin/figurehead"
                     path (FilenameUtils/getFullPath apk-path)]
                 (send-root-command :su-instance su-instance
                                    :commands [(str "echo '# bootstrapping Figurehead' > "
                                                    figurehead-script)
                                               (str "echo 'export CLASSPATH=" apk-path "' >> "
                                                    figurehead-script)
                                               (str "echo 'exec app_process " path " figurehead.main \"$@\"' >> "
                                                    figurehead-script)
                                               (str "chmod 700 "
                                                    figurehead-script)])
                 (deliver (get-app-info-entry :figurehead-script)
                          figurehead-script))))

           (catch Exception e
             (neko.log/e
              (with-out-str (print-stack-trace e))))))
       (do
         ;; no SU
         (set-app-info-entry :su? false)
         (on-ui
          (toast "no Superuser")))))

    (background-thread
     (start-repl :port 9999))

    (on-ui
     (set-content-view! this
                        R$layout/main)))

  :on-start
  (fn [^Activity this]
    (with-activity this
      (let [widgets {:figurehead-switch  ^Switch (find-view R$id/figurehead_switch)
                     :monitor ^CheckBox (find-view R$id/monitor)
                     :verbose ^CheckBox (find-view R$id/verbose)
                     :repl-port ^EditText (find-view R$id/repl_port)
                     :mastermind-address ^EditText (find-view R$id/mastermind_address)
                     :mastermind-port ^EditText (find-view R$id/mastermind_port)
                     :scroll-status ^ScrollView (find-view R$id/scroll_status)
                     :status ^TextView (find-view R$id/status)
                     :clear-status ^Button (find-view R$id/clear_status)}]
        (with-widgets widgets
          (update-figurehead-switch widgets)

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
                (if is-checked?
                  (do
                    ;; turn on
                    (set-enabled widgets false)
                    (let [figurehead-args  (into ["--replace"]
                                                 (widgets-to-figurehead-args widgets))]
                      (neko.log/e
                       (with-out-str
                         (pprint figurehead-args)))
                      (execute-figurehead-command
                       {:args figurehead-args
                        :buffered? false}
                       (on-ui
                        (.append widget-status (with-out-str (println line)))
                        (.post widget-scroll-status
                               #(.fullScroll widget-scroll-status
                                             View/FOCUS_DOWN)))))
                    (.setEnabled widget-figurehead-switch true)
                    (.setEnabled widget-scroll-status true)
                    (.setEnabled widget-status true)
                    (.setEnabled widget-clear-status true)) 
                  (do
                    ;; turn off
                    (set-enabled widgets false)
                    (execute-figurehead-command
                     {:args "--kill"
                      :buffered? true}
                     (set-enabled widgets true))))))))))))

  :on-destroy
  (fn [^Activity this]
    (background-thread (stop-repl))
    (toast "Figurehead destroyed")))
