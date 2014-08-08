(ns figurehead.ui.helper.widgets
  "widgets helpers"
  (:use (figurehead.ui su
                       util)
        (figurehead.ui.helper figurehead))
  (:require (neko [threading :refer [on-ui]]
                  [notify :refer [toast]]))
  (:require (clojure [string :as str]
                     [set :as set]
                     [pprint :refer [pprint]]
                     [stacktrace :refer [print-stack-trace]]))
  (:import (android.widget Switch
                           Button
                           CheckBox
                           EditText
                           TextView
                           ScrollView)
           (android.view View)))

(declare with-widgets set-enabled
         sync-widgets-to-state sync-widgets-to-figurehead
         widgets-to-arg-map widgets-to-figurehead-args
         save-widget-state get-saved-widget-state)

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

(defn sync-widgets-to-state
  "sync widgets to state"
  [widgets state]
  (on-ui
   (with-widgets widgets
     (try
       ;; temporarily disable all widgets during state transition
       (set-enabled widgets false)
       (try
         (when-not (nil? (:monitor state))
           (.setChecked widget-monitor
                        (Boolean/parseBoolean ^String (str (:monitor state)))))
         (catch Exception e))
       (try
         (when-not (nil? (:verbose state))
           (.setChecked widget-verbose
                        (Boolean/parseBoolean ^String (str (:verbose state)))))
         (catch Exception e))
       (try
         (when-not (nil? (:nrepl-port state))
           (.setText widget-repl-port
                     ^String
                     (let [nrepl-port ^String (str (:nrepl-port state))]
                       (if nrepl-port nrepl-port ""))))
         (catch Exception e))
       (try
         (when-not (nil? (:mastermind-address state))
           (.setText widget-mastermind-address
                     ^String
                     (let [mastermind-address ^String (str (:mastermind-address state))]
                       (if mastermind-address mastermind-address ""))))
         (catch Exception e))
       (try
         (when-not (nil? (:mastermind-port state))
           (.setText widget-mastermind-port
                     ^String
                     (let [mastermind-port ^String (str (:mastermind-port state))]
                       (if mastermind-port mastermind-port ""))))
         (catch Exception e))
       (try
         (when-not (nil? (:is-running? state))
           (.setChecked widget-figurehead-switch
                        (Boolean/parseBoolean ^String (str (:is-running? state)))))
         (catch Exception e))         
       (catch Exception e
         (print-stack-trace e))
       (finally
         ;; enable needed widgets
         (if (.isChecked widget-figurehead-switch)
           (do
             (.setEnabled widget-figurehead-switch true)
             (.setEnabled widget-scroll-status true)
             (.setEnabled widget-status true)
             (.setEnabled widget-clear-status true))
           (do
             (set-enabled widgets true))))))))

(def ^:private first-sync?
  "the first sync should be from SU figurehead; later can from saved widget state"
  (atom true))

(defn sync-widgets-to-figurehead
  "sync widgets status to figurehead"
  [widgets]
  (background-thread
   (let [saved-widget-state (get-saved-widget-state)]
     (sync-widgets-to-state widgets
                            (if (and saved-widget-state
                                     ;; force to sync at least once
                                     (not @first-sync?))
                              saved-widget-state
                              (do
                                (let [state (get-running-figurehead-state)
                                      is-running? (if (:is-running? state) true false)]
                                  (assoc (:state state)
                                    :is-running? is-running?))
                                (reset! first-sync? false)))))))

(defn widgets-to-arg-map
  "convert widgets to Figurehead argument map"
  [widgets]
  (let [args (atom {})]
    (with-widgets widgets
      (try
        (let [checked (.isChecked widget-monitor)]
          (swap! args assoc :monitor checked))
        (catch Exception e
          (print-stack-trace e)))
      (try
        (let [checked (.isChecked widget-verbose)]
          (swap! args assoc :verbose checked))
        (catch Exception e
          (print-stack-trace e)))
      (try
        (let [port (int (read-string (.. widget-repl-port getText toString trim)))]
          (if (< 0 port 65536)
            (do
              (swap! args assoc :repl-port port))
            (do
              (on-ui
               (.setText widget-repl-port "")))))
        (catch Exception e
          (print-stack-trace e)
          (on-ui
           (.setText widget-repl-port ""))))
      (try
        (let [text (read-string (str "\""
                                     (.. widget-mastermind-address getText toString trim)
                                     "\""))]
          (when-not (empty? text)
            (swap! args assoc :mastermind-address text)))
        (catch Exception e
          (print-stack-trace e)
          (on-ui
           (.setText widget-mastermind-address ""))))
      (try
        (let [port (int (read-string (.. widget-mastermind-port getText toString trim)))]
          (if (< 0 port 65536)
            (do
              (swap! args assoc :mastermind-port port))
            (do
              (on-ui
               (.setText widget-mastermind-port "")))))
        (catch Exception e
          (print-stack-trace e)
          (on-ui
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

(def ^:private saved-widget-state
  "the saved widget state"
  (atom {}))

(defn save-widget-state
  [widgets]
  (with-widgets widgets
    (try
      (swap! saved-widget-state
             assoc :monitor (.isChecked widget-monitor))
      (swap! saved-widget-state
             assoc :verbose (.isChecked widget-verbose))
      (swap! saved-widget-state
             assoc :nrepl-port (str (.getText widget-repl-port)))
      (swap! saved-widget-state
             assoc :mastermind-address (str (.getText widget-mastermind-address)))
      (swap! saved-widget-state
             assoc :mastermind-port (str (.getText widget-mastermind-port)))
      (swap! saved-widget-state
             assoc :is-running? (.isChecked widget-figurehead-switch))      
      (catch Exception e
        (reset! saved-widget-state nil)))))

(defn get-saved-widget-state
  "obtain the saved widget state"
  []
  @saved-widget-state)
