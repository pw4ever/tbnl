(ns figurehead.ui.su
  (:require [clojure.string :as str])
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
         send-root-command)

(def root-shell (atom nil))

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

(defn ^Shell$Interactive open-root-shell
  "open a new root shell"
  [&
   {:keys [on-shell-running
           watch-dog-timeout
           command-result-listener
           want-stderr?
           minimal-logging?]
    :or {watch-dog-timeout 0
         want-stderr? false
         minimal-logging? true}
    :as args}]
  (let [su (.. (Shell$Builder.)
               (useSU)
               (setWantSTDERR want-stderr?)
               (setMinimalLogging minimal-logging?)
               (setWatchdogTimeout watch-dog-timeout)
               (open (proxy [Shell$OnCommandResultListener] []
                       (onCommandResult [command-code exit-code ^List output]
                         (when command-result-listener
                           (command-result-listener command-code exit-code output))
                         
                         (case exit-code
                           
                           Shell$OnCommandResultListener/SHELL_RUNNING
                           (do
                             (when on-shell-running
                               (on-shell-running :command-code command-code
                                                 :exit-code exit-code
                                                 :output output)))

                           Shell$OnCommandResultListener/WATCHDOG_EXIT
                           (do
                             (reset! root-shell nil))

                           Shell$OnCommandResultListener/SHELL_DIED
                           (do
                             (reset! root-shell nil))

                           Shell$OnCommandResultListener/SHELL_EXEC_FAILED
                           (do
                             (reset! root-shell nil))

                           Shell$OnCommandResultListener/SHELL_WRONG_UID
                           (do
                             (reset! root-shell nil))

                           (do
                             (reset! root-shell nil)))))))]
    (reset! root-shell su)
    su))

(defn send-root-command
  "send commands to the root shell instance su-instance, create it if nil"
  [&
   {:keys [su-instance
           commands
           command-code
           command-result-listener
           command-line-listener]
    :or {command-code 0
         su-instance @root-shell}
    :as args}]
  (let [real-su (promise)]
    (if su-instance
      (deliver real-su su-instance)
      (open-root-shell :on-shell-running
                       (fn [& {:keys [command-code exit-code output]
                               :as args}]
                         (deliver real-su @root-shell))))
    (when-let [^Shell$Interactive su-instance @real-su]
      (let [commands (cond (string? commands)
                           [commands]

                           :else
                           commands)]
        (doseq [command commands]
          (let [command (str command)]
            (cond command-line-listener
                  (do
                    (.addCommand su-instance
                                 ^String command
                                 ^int command-code
                                 (proxy [Shell$OnCommandLineListener] []
                                   (onLine [^String line]
                                     (command-line-listener line))
                                   (onCommandResult [command-code exit-code]
                                     (when command-result-listener
                                       (command-result-listener command-code
                                                                exit-code))))))


                  command-result-listener
                  (do
                    (.addCommand su-instance
                                 ^String command
                                 ^int command-code
                                 (proxy [Shell$OnCommandResultListener] []
                                   (onCommandResult [command-code exit-code ^List output]
                                     (command-result-listener command-code
                                                              exit-code
                                                              output)))))

                  :else
                  (do
                    (.addCommand su-instance
                                 ^String command)))))))))
