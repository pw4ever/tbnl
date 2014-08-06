(ns figurehead.ui.helper
  "helpers"
  (:use (figurehead.ui su
                       util))
  (:require (neko init
                  [notify :refer [toast]]
                  [threading :refer [on-ui]]))
  (:require (clojure [string :as str]
                     [set :as set]
                     [stacktrace :refer [print-stack-trace]]))
  (:require [clojure.core.async :as async])
  (:require [clojure.tools.nrepl.server :as nrepl-server]
            (cider.nrepl.middleware apropos
                                    classpath
                                    ;;complete
                                    ;;info
                                    inspect
                                    macroexpand
                                    resource
                                    stacktrace
                                    test
                                    ;;trace
                                    ))
  (:import (android.content Context))
  (:import (android.widget Switch
                           Button
                           CheckBox
                           EditText
                           TextView
                           ScrollView)
           (android.view View)
           (java.util List))
  (:import (figurehead.ui R$layout
                          R$id)))

(declare
 ;; work with figurehead
 get-figurehead-apk-path build-figurehead-command
 execute-figurehead-command
 ;; widgets
 with-widgets
 set-enabled update-figurehead-switch
 widgets-to-arg-map widgets-to-figurehead-args
 ;; REPL
 start-repl stop-repl)

;;; work with figurehead

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
  (let [figurehead-script @(get-app-info-entry :figurehead-script)]
    (str/join " " (into [figurehead-script] commands))))

(defmacro execute-figurehead-command
  "execute figurehead command with call back body"
  [{:keys [args
           buffered?]
    :or {args []
         buffered? true}
    :as options}
   & callback-body]
  `(background-thread
    (try
      (let [su-instance# (open-root-shell)
            args# ~args
            command# (apply build-figurehead-command
                            (if (sequential? args#)
                              args#
                              [args#]))]
        (on-ui
         (toast (str "Executing: " command#)))
        (apply send-root-command
               :su-instance su-instance#
               :commands [command#]
               ~(if buffered?
                  `[:command-result-listener
                    (fn [~'command-code ~'exit-code ^List ~'output]
                      (when (>= ~'exit-code 0)
                        ~@callback-body))]
                  `[:command-line-listener
                    (fn [^String ~'line]
                      ~@callback-body)])))
      (catch Exception e#
        (print-stack-trace e#)))))

;;; widgets

(defmacro with-widgets
  "wrap body with widgets tagged bindings"
  [widgets & body]
  `(let [widgets# ~widgets
         ~'widget-figurehead-switch ^Switch (:figurehead-switch widgets#)
         ~'widget-monitor ^CheckBox (:monitor widgets#)
         ~'widget-verbose ^CheckBox (:verbose widgets#)
         ~'widget-repl-port ^EditText (:repl-port widgets#)
         ~'widget-mastermind-address ^EditText (:mastermind-address widgets#)
         ~'widget-mastermind-port ^EditText (:mastermind-port widgets#)
         ~'widget-status ^TextView (:status widgets#)
         ~'widget-scroll-status ^ScrollView (:scroll-status widgets#)
         ~'widget-clear-status ^Button (:clear-status widgets#)]
     ~@body))

(defn set-enabled
  "set enabled status of the widgets"
  [widgets enabled]
  (on-ui
   (doseq [[_ ^View widget] widgets]
     (.setEnabled widget enabled))))

(defn update-figurehead-switch
  "check whether figurehead is running and update widgets"
  [widgets]
  (set-enabled widgets false)
  (with-widgets widgets
    (execute-figurehead-command
     {:args "--status"
      :buffered? true}
     (let [output (str/join " " output)]
       (try
         (let [output (read-string output)
               is-running? (:is-running? output)]
           (on-ui
            (if is-running?
              (do
                ;; is-running?
                (let [state (:state output)
                      monitor (Boolean/parseBoolean (:monitor state))
                      nrepl-port ^String (:nrepl-port state)
                      mastermind-address ^String (:mastermind-address state)
                      mastermind-port ^String (:mastermind-port state)]
                  ;; set widgets to current status
                  (.setChecked widget-monitor
                               monitor)
                  (.setText widget-repl-port
                            (if (and nrepl-port (not (empty? nrepl-port)))
                              nrepl-port
                              ""))
                  (.setText widget-mastermind-address
                            (if (and mastermind-address (not (empty? mastermind-address)))
                              mastermind-address
                              ""))
                  (.setText widget-mastermind-port
                            (if (and mastermind-port (not (empty? mastermind-port)))
                              mastermind-port
                              ""))))
              (do
                ;; (not is-running?)
                (set-enabled widgets true)))
            (.setChecked widget-figurehead-switch is-running?)
            (.setEnabled widget-figurehead-switch true)
            (.setEnabled widget-scroll-status true)
            (.setEnabled widget-status true)
            (.setEnabled widget-clear-status true)))
         (catch Exception e
           (print-stack-trace e)))))))



(defn widgets-to-arg-map
  "convert widgets to Figurehead argument map"
  [widgets]
  (let [args (atom {})]
    (with-widgets widgets
      (on-ui
       (try
         (let [checked (.isChecked widget-monitor)]
           (swap! args assoc :monitor checked))
         (catch Exception e))
       (try
         (let [checked (.isChecked widget-verbose)]
           (swap! args assoc :verbose checked))
         (catch Exception e))
       (try
         (let [port (int (read-string (.. widget-repl-port getText toString trim)))]
           (if (< 0 port 65536)
             (do
               (swap! args assoc :repl-port port))
             (do
               (.setText widget-repl-port ""))))
         (catch Exception e
           (.setText widget-repl-port "")))
       (try
         (let [text (read-string (str "\""
                                      (.. widget-mastermind-address getText toString trim)
                                      "\""))]
           (when-not (empty? text)
             (swap! args assoc :mastermind-address text)))
         (catch Exception e
           (.setText widget-mastermind-address "")))
       (try
         (let [port (int (read-string (.. widget-mastermind-port getText toString trim)))]
           (if (< 0 port 65536)
             (do
               (swap! args assoc :mastermind-port port))
             (do
               (.setText widget-mastermind-port ""))))
         (catch Exception e
           (.setText widget-mastermind-port "")))))
    @args))

(defn widgets-to-figurehead-args
  "construct figurehead command-line args from widgets"
  [widgets]
  (let [args (atom [])
        arg-map (widgets-to-arg-map widgets)
        monitor (:monitor arg-map)
        verbose (:verbose arg-map)
        repl-port (:repl-port arg-map)
        mastermind-address (:mastermind-address arg-map)
        mastermind-port (:mastermind-port arg-map)]
    (when monitor
      (swap! args conj
             "--monitor"))
    (when verbose
      (swap! args conj
             "--verbose"))
    (when repl-port
      (swap! args conj
             (str "--nrepl-port " repl-port)))
    (when mastermind-address
      (swap! args conj
             (str "--mastermind-address " mastermind-address)))
    (when mastermind-port
      (swap! args conj
             (str "--mastermind-port" mastermind-port)))
    @args))

;;; REPL

(def repl-session
  "the sole REPL session to figurehead.ui"
  (atom nil))

(def ^:private cider-middleware
  "A vector containing all CIDER middleware."
  '[cider.nrepl.middleware.apropos/wrap-apropos
    cider.nrepl.middleware.classpath/wrap-classpath
    ;;cider.nrepl.middleware.complete/wrap-complete
    ;;cider.nrepl.middleware.info/wrap-info
    cider.nrepl.middleware.inspect/wrap-inspect
    cider.nrepl.middleware.macroexpand/wrap-macroexpand
    cider.nrepl.middleware.resource/wrap-resource
    cider.nrepl.middleware.stacktrace/wrap-stacktrace
    cider.nrepl.middleware.test/wrap-test
    ;;cider.nrepl.middleware.trace/wrap-trace
    ])

(def ^:private cider-nrepl-handler
  "CIDER's nREPL handler."
  (apply nrepl-server/default-handler (map resolve cider-middleware)))

(defn start-repl
  "start REPL is there is none"
  [& {:keys [port]
      :or {port 9999}
      :as args}]
  (when-not @repl-session
    (background-thread
     (reset! repl-session
             (neko.init/start-repl :port port
                                   :handler cider-nrepl-handler))
     ;; set up 'user ns
     (do
       (in-ns 'user)
       (use 'clojure.repl)
       (use 'clojure.pprint)
       (use 'clojure.java.io)
       (require 'clojure.set :as set)

       (use 'neko.doc)
       (use 'neko.debug)))))

(defn stop-repl
  "stop the sole"
  []
  (let [session @repl-session]
    (when session
      (background-thread
       (reset! repl-session nil)
       (nrepl-server/stop-server session)))))
