(ns figurehead.ui.su
  (:use (figurehead.ui util))
  (:require (neko [notify :refer [toast]]
                  [threading :refer [on-ui]]
                  [log :as log]))
  (:require (clojure [string :as str]
                     [stacktrace :refer [print-stack-trace]]))
  (:import (android.content Context))
  (:import (java.util Collection
                      ArrayList
                      List))
  (:import eu.chainfire.libsuperuser.Shell
           eu.chainfire.libsuperuser.Shell$SU
           eu.chainfire.libsuperuser.Shell$SH
           eu.chainfire.libsuperuser.Shell$Interactive
           eu.chainfire.libsuperuser.Shell$Builder
           eu.chainfire.libsuperuser.Shell$OnCommandLineListener
           eu.chainfire.libsuperuser.Shell$OnCommandResultListener))

(declare su?
         su sh
         open-root-shell
         execute-root-command)

(defn su?
  "check whether SuperUser is available"
  []
  (Shell$SU/available))

(defn su
  "run cmds with Super User"
  [& commands]
  (when commands
    (Shell$SU/run (ArrayList. ^Collection commands))))

(defn sh
  "run cmds with SHell"
  [& commands]
  (when commands
    (Shell$SH/run (ArrayList. ^Collection commands))))

(defmacro open-root-shell
  "open a new root shell"
  [&
   {:keys [timeout
           want-stderr?
           minimal-logging?
           callback?
           
           command-result-listener

           on-shell-running
           on-watchdog-exit
           on-shell-died
           on-shell-exec-failed
           on-shell-wrong-uid
           on-default
           
           on-error
           on-normal

           error-message]
    :or {timeout 0
         want-stderr? false
         minimal-logging? true
         callback? false
         error-message "open-root-shell"}
    :as args}]
  `(let [timeout# ~timeout
         want-stderr?# ~want-stderr?
         minimal-logging?# ~minimal-logging?
         ~'error-message ~error-message
         su# (promise)]
     (background-looper-thread
      (deliver su#
               (.. (Shell$Builder.)
                   (useSU)
                   (setAutoHandler true)
                   (setWatchdogTimeout timeout#)
                   (setWantSTDERR want-stderr?#)
                   (setMinimalLogging minimal-logging?#)
                   (open
                    ~(when callback?
                       `(proxy [Shell$OnCommandResultListener] []
                          (onCommandResult [~'command-code
                                            ~'exit-code
                                            ^List ~'output]
                            ~command-result-listener

                            (if (>= ~'exit-code 0)
                              (do
                                ;; normal
                                ~on-normal)
                              (do
                                ;; error
                                (let [error# (str ~'error-message
                                                  " "
                                                  ~'exit-code)]
                                  (neko.threading/on-ui
                                   (neko.notify/toast error#))
                                  (neko.log/e error#))
                                ~on-error))

                            (case ~'exit-code
                                             
                              Shell$OnCommandResultListener/SHELL_RUNNING
                              (do
                                ~on-shell-running)

                              Shell$OnCommandResultListener/WATCHDOG_EXIT
                              (do
                                (let [error# (str ~'error-message
                                                  " (timeout)")]
                                  (neko.threading/on-ui
                                   (neko.notify/toast error#))
                                  (neko.log/e error#))
                                ~on-watchdog-exit)

                              Shell$OnCommandResultListener/SHELL_DIED
                              (do
                                (let [error# (str ~'error-message
                                                  " (died)")]
                                  (neko.threading/on-ui
                                   (neko.notify/toast error#))
                                  (neko.log/e error#))
                                ~on-shell-died)

                              Shell$OnCommandResultListener/SHELL_EXEC_FAILED
                              (do
                                (let [error# (str ~'error-message
                                                  " (exec failed)")]
                                  (neko.threading/on-ui
                                   (neko.notify/toast error#))
                                  (neko.log/e error#))
                                ~on-shell-exec-failed)

                              Shell$OnCommandResultListener/SHELL_WRONG_UID
                              (do
                                (let [error# (str ~'error-message
                                                  " (wrong uid)")]
                                  (neko.threading/on-ui
                                   (neko.notify/toast error#))
                                  (neko.log/e error#))
                                ~on-shell-wrong-uid)

                              ;; default clause
                              ;; should not reach here
                              (do
                                ~on-default)))))))))
     @su#))

(defmacro execute-root-command
  "execute root command"
  [&
   {:keys [commands
           timeout
           command-code
           callback?
           buffered?
           
           command-result-listener
           command-line-listener

           on-shell-running
           on-watchdog-exit
           on-shell-died
           on-shell-exec-failed
           on-shell-wrong-uid
           on-default
           
           on-error
           on-normal

           error-message]
    :or {commands []
         timeout 0
         command-code 0
         callback? false
         buffered? true
         error-message "execute-root-command"}
    :as args}]

  `(background-looper-thread
    (try
      (let [commands# ~commands
            timeout# ~timeout
            ~'command-code ~command-code
            ~'error-message ~error-message]
        (let [^Shell$Interactive
              ~'su-instance (open-root-shell
                             :timeout timeout#)]
          (try
            (let [commands# (if (sequential? commands#)
                              commands#
                              [commands#])]
              (doseq [command# commands#]
                (let [~'command (str command#)]
                  (let [info# (str "SU: " ~'command)]
                    (log/i info#))
                  ~(if callback?
                     (do
                       (if buffered?
                         (do
                           ;; process buffered output
                           `(.addCommand ^Shell$Interactive
                                         ~'su-instance
                                         ^String
                                         ~'command
                                         ~'command-code
                                         (proxy [Shell$OnCommandResultListener] []
                                           (onCommandResult [~'command-code
                                                             ~'exit-code
                                                             ^List ~'output]
                                             ~command-result-listener

                                             (if (>= ~'exit-code 0)
                                               (do
                                                 ;; normal
                                                 ~on-normal)
                                               (do
                                                 ;; error
                                                 (let [error# (str ~'error-message
                                                                   " "
                                                                   ~'exit-code)]
                                                   (neko.threading/on-ui
                                                    (neko.notify/toast error#))
                                                   (neko.log/e error#))
                                                 ~on-error))

                                             (case ~'exit-code
                                             
                                               Shell$OnCommandResultListener/SHELL_RUNNING
                                               (do
                                                 ~on-shell-running)

                                               Shell$OnCommandResultListener/WATCHDOG_EXIT
                                               (do
                                                 (let [error# (str ~'error-message
                                                                   " (timeout)")]
                                                   (neko.threading/on-ui
                                                    (neko.notify/toast error#))
                                                   (neko.log/e error#))
                                                 ~on-watchdog-exit)

                                               Shell$OnCommandResultListener/SHELL_DIED
                                               (do
                                                 (let [error# (str ~'error-message
                                                                   " (died)")]
                                                   (neko.threading/on-ui
                                                    (neko.notify/toast error#))
                                                   (neko.log/e error#))
                                                 ~on-shell-died)

                                               Shell$OnCommandResultListener/SHELL_EXEC_FAILED
                                               (do
                                                 (let [error# (str ~'error-message
                                                                   " (exec failed)")]
                                                   (neko.threading/on-ui
                                                    (neko.notify/toast error#))
                                                   (neko.log/e error#))
                                                 ~on-shell-exec-failed)

                                               Shell$OnCommandResultListener/SHELL_WRONG_UID
                                               (do
                                                 (let [error# (str ~'error-message
                                                                   " (wrong uid)")]
                                                   (neko.threading/on-ui
                                                    (neko.notify/toast error#))
                                                   (neko.log/e error#))
                                                 ~on-shell-wrong-uid)

                                               ;; default clause
                                               ;; should not reach here
                                               (do
                                                 ~on-default))))))
                         (do
                           ;; process output line by line
                           `(.addCommand ^Shell$Interactive
                                         ~'su-instance
                                         ^String
                                         ~'command
                                         ~'command-code
                                         (proxy [Shell$OnCommandLineListener] []
                                         
                                           (onLine [^String ~'line]
                                             ~command-line-listener)
                                         
                                           (onCommandResult [~'command-code
                                                             ~'exit-code]
                                             ~command-result-listener

                                             (if (>= ~'exit-code 0)
                                               (do
                                                 ;; normal
                                                 ~on-normal)
                                               (do
                                                 ;; error
                                                 (let [error# (str ~'error-message
                                                                   " "
                                                                   ~'exit-code)]
                                                   (neko.threading/on-ui
                                                    (neko.notify/toast error#))
                                                   (neko.log/e error#))
                                                 ~on-error))

                                             (case ~'exit-code
                                             
                                               Shell$OnCommandLineListener/SHELL_RUNNING
                                               (do
                                                 ~on-shell-running)

                                               Shell$OnCommandLineListener/WATCHDOG_EXIT
                                               (do
                                                 (let [error# (str ~'error-message
                                                                   " (timeout)")]
                                                   (neko.threading/on-ui
                                                    (neko.notify/toast error#))
                                                   (neko.log/e error#))
                                                 ~on-watchdog-exit)

                                               Shell$OnCommandLineListener/SHELL_DIED
                                               (do
                                                 (let [error# (str ~'error-message
                                                                   " (died)")]
                                                   (neko.threading/on-ui
                                                    (neko.notify/toast error#))
                                                   (neko.log/e error#))
                                                 ~on-shell-died)

                                               Shell$OnCommandLineListener/SHELL_EXEC_FAILED
                                               (do
                                                 (let [error# (str ~'error-message
                                                                   " (exec failed)")]
                                                   (neko.threading/on-ui
                                                    (neko.notify/toast error#))
                                                   (neko.log/e error#))
                                                 ~on-shell-exec-failed)

                                               Shell$OnCommandLineListener/SHELL_WRONG_UID
                                               (do
                                                 (let [error# (str ~'error-message
                                                                   " (wrong uid)")]
                                                   (neko.threading/on-ui
                                                    (neko.notify/toast error#))
                                                   (neko.log/e error#))
                                                 ~on-shell-wrong-uid)

                                               ;; default clause
                                               ;; should not reach here
                                               (do
                                                 ~on-default))))))))
                     (do
                       `(.addCommand ^Shell$Interactive
                                     ~'su-instance
                                     ^String
                                     ~'command))))))
            (finally
              (.close ^Shell$Interactive
                      ~'su-instance)))))
      (catch Exception e#
        (print-stack-trace e#)))))
