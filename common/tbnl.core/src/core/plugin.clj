(ns core.plugin
  (:require (core [bus :as bus]))
  (:require [clojure.stacktrace :refer [print-stack-trace]]
            [clojure.core.async
             :as async
             :refer [thread <!! chan timeout]]))

(declare list-all-plugins list-all-plugins-by-priority
         get-plugin get-plugin-main-entry
         get-config-map get-config-map-entry update-config-map-entry set-config-map-entry
         get-param get-param-entry update-param-entry set-param-entry
         get-state get-state-entry update-state-entry set-state-entry
         load-plugin unload-plugin
         populate-parse-opts-vector
         init-plugin run-plugin stop-plugin init-and-run-plugin load-init-and-run-plugin restart-plugin
         block-thread unblock-thread
         register-exit-hook unregister-exit-hook execute-all-exit-hooks)

(def ^:dynamic *current-plugin*
  "bound to current plugin by the context")

(def defaults
  "the defaults"
  (atom {
         :auto-restart-retry-interval 1000
         }))

(def plugins
  "all loaded plugins and their config map"
  (atom {}))

(def current-options
  "current options"
  (atom nil))

(def exit-hooks
  "exit hooks that are executed on main-thread exit"
  (atom {}))

(defn list-all-plugins
  "list all plugins"
  []
  (keys @plugins))

(defn list-all-plugins-by-priority
  "list all plugins from highest (larger number) to lowest priority

the convention is that 'I do not care'-priority is 1, and 'absolute first'-priority is 99; 0 and 100 are reserved for core"
  []
  (->> @plugins
       (sort-by #(or (get-in (second %)
                             [:param :priority])
                     ;; assign low priority if missing :priority spec
                     0))
       (map first)
       reverse))

(defn list-all-nonstop-plugins-by-priority
  "list all non-stop plugins from highest to lowest priority"
  []
  (filter #(not (get-state-entry % :stop))
          (list-all-plugins-by-priority)))

(defn get-plugin
  "get the named plugin"
  ([] (get-plugin *current-plugin*))
  ([plugin]
     (keyword plugin)))

(defn get-plugin-main-entry
  "get the main entry to the plugin"
  ([] (get-plugin-main-entry *current-plugin*))
  ([plugin]
     (symbol (str plugin ".main"))))

;;; config-map

(defn get-config-map 
  "get the config map"
  ([] (get-config-map *current-plugin*))
  ([plugin]
     (get @plugins (get-plugin plugin))))

(defn get-config-map-entry 
  "get a config entry"
  ([key] (get-config-map-entry *current-plugin* key))
  ([plugin key]
     (get (get-config-map plugin) key)))

(defn update-config-map-entry
  "update a config entry to (f <current entry> & args)"
  ([plugin key f & args]
     (apply swap! plugins update-in 
            [(get-plugin plugin) key]
            f args)))

(defn set-config-map-entry
  "set a config entry to val"
  ([key val] (set-config-map-entry *current-plugin* key val))
  ([plugin key val]
     (update-config-map-entry plugin key (constantly val))))

;;; config-map/param
(defn get-param
  "get the params"
  ([] (get-param *current-plugin*))
  ([plugin]
     (get-config-map-entry plugin :param)))

(defn get-param-entry
  "get a param entry"
  ([] (get-param-entry *current-plugin*))
  ([plugin key]
     (get (get-param plugin) key)))

(defn update-param-entry
  "update a param entry to (f <current entry> & args)"
  ([plugin key f & args]
     (apply swap! plugins update-in
            [(get-plugin plugin) :param key]
            f args)))

(defn set-param-entry
  "set a parameter entry to val"
  ([key val] (set-param-entry *current-plugin* key val))
  ([plugin key val]
     (update-param-entry plugin key (constantly val))))

;;; config-map/state
(defn get-state
  "get the states"
  ([] (get-state *current-plugin*))
  ([plugin]
     (get-config-map-entry plugin :state)))

(defn get-state-entry
  "get a state entry"
  ([key] (get-state-entry *current-plugin* key))
  ([plugin key]
     (get (get-state plugin) key)))

(defn update-state-entry
  "update a state entry to (f <current entry> & args)"
  ([plugin key f & args]
     (apply swap! plugins update-in
            [(get-plugin plugin) :state key]
            f args)))

(defn set-state-entry
  "set a state entry to val"
  ([key val] (set-state-entry *current-plugin* key val))
  ([plugin key val]
     (update-state-entry plugin key (constantly val))))

;;; load/unload plugin

(defn load-plugin
  "load a plugin"
  [plugin]
  (binding [*current-plugin* plugin]
    (let [plugin-main-entry (get-plugin-main-entry plugin)]
      (require plugin-main-entry)
      (swap! plugins
             assoc
             (get-plugin plugin)
             @(ns-resolve plugin-main-entry
                          'config-map)))))

(defn unload-plugin
  "unload the plugins"
  [plugin]
  (binding [*current-plugin* plugin]
    (when-let [unload (get-config-map-entry plugin :unload)]
      (unload))
    (swap! plugins
           dissoc
           (get-plugin plugin))))

;;; populate parse-opts vector
(defn populate-parse-opts-vector 
  "populate parse-opts vector for the plugin"
  [plugin current-parse-opts-vector]
  (binding [*current-plugin* plugin]
    (when-let [populate-parse-opts-vector (get-config-map-entry plugin :populate-parse-opts-vector)]
      (populate-parse-opts-vector current-parse-opts-vector))))

;;; init/run plugin
(defn init-plugin 
  "initialize the plugin with the options; return false to abort running the plugin"
  ([plugin] (init-plugin plugin @current-options))
  ([plugin options]
     (binding [*current-plugin* plugin]
       (let [verbose (:verbose options)]
         (when verbose
           (prn [:init-plugin plugin options]))
         (let [result (if-let [init (get-config-map-entry plugin :init)]
                        (init options)
                        true)]
           (when verbose
             (prn [:init-plugin plugin :result result]))
           result)))))

(defn run-plugin 
  "run the plugin with the options in a separte thread"
  ([plugin] (run-plugin plugin @current-options))
  ([plugin options]
     (binding [*current-plugin* plugin]
       (let [verbose (:verbose options)]
         (when-let [run (get-config-map-entry plugin :run)]
           (when verbose
             (prn [:run-plugin plugin options]))
           (set-state-entry plugin :stop false)
           (if (get-param-entry plugin :sync)
             (when-not (get-state-entry plugin :stop)
               (try
                 (run options)
                 (catch Exception e
                   (when verbose
                     (print-stack-trace e)))))
             (thread
               ;; only :async plugin can auto-restart
               (loop []
                 (try
                   (run options)
                   (catch Exception e
                     (when verbose
                       (print-stack-trace e))))
                 (when (and
                        ;; auto-restart has been requested and...
                        (get-param-entry plugin :auto-restart)
                        ;; plugin has NOT been explicitly stopped
                        (not (get-state-entry plugin :stop)))
                   (let [auto-restart-retry-interval (:auto-restart-retry-interval @defaults)]
                     (when verbose
                       (prn [:plugin plugin
                             :auto-restart
                             :retry-interval auto-restart-retry-interval]))
                     (Thread/sleep auto-restart-retry-interval)
                     (recur)))))))))))

(defn stop-plugin
  "stop the plugin"
  ([plugin] (stop-plugin plugin @current-options))
  ([plugin options]
     (let [verbose (:verbose options)
           stop (get-config-map-entry plugin :stop)]
       (when stop
         (when verbose
           (prn [:stop-plugin plugin options]))
         (stop options)))))

(defn init-and-run-plugin
  "init the plugin and, if successful, run it"
  ([plugin] (init-and-run-plugin plugin @current-options))
  ([plugin options]
     (let [verbose (:verbose options)]
       (when (init-plugin plugin options)
         (run-plugin plugin options)
         (when-let [wait (get-param-entry plugin :wait)]
           (when verbose
             (prn [:wait wait]))
           (<!! (timeout wait)))))))

(defn load-init-and-run-plugin
  "load-plugin + init-and-run-plugin; mainly for dynamic loading"
  [plugin options]
  (let [verbose (:verbose options)]
    (load-plugin plugin)
    (init-and-run-plugin plugin options)))


(defn restart-plugin
  "stop-plugin + init-and-run-plugin"
  ([plugin] (restart-plugin plugin @current-options))
  ([plugin options]
     (stop-plugin plugin options)
     (init-and-run-plugin plugin options)))

(defn block-thread
  "block the plugin thread so will not keep restarting the plugin; optional with timeout and topic (subscribed to :unblock-thread"
  ([] (block-thread nil nil))
  
  ([timeout-or-tag]
     (cond
      (number? timeout-or-tag)
      (block-thread timeout-or-tag nil)

      :else
      (block-thread nil timeout-or-tag)))

  ([timeout tag]
     (let [ch (if (number? timeout)
                (async/timeout timeout)
                (chan))]
       (try
         (when tag
           (bus/sub-topic ch :unblock-thread))
         (loop [said (bus/what-is-said!! ch)]
           (when (and said tag (not= tag said))
             ;; only recur when <!! return from :unblock-thread (and val tag) and not having a matching tag
             (recur (bus/what-is-said!! ch))))
         (finally
           (when tag
             (bus/unsub-topic ch :nnblock-thread)))))))

(defn unblock-thread
  "unblock thread with the given unblock-tag"
  [unblock-tag]
  (bus/say!! :unblock-thread unblock-tag))

(defmacro blocking-jail
  "jail body by blocking"
  [[timeout unblock-tag finalization verbose] & body]
  `(let [timeout# ~timeout
         unblock-tag# ~unblock-tag
         verbose# ~verbose]
     (try
       ~@body
       (block-thread timeout# unblock-tag#)
       
       (catch Exception e#
         (when verbose#
           (clojure.stacktrace/print-stack-trace e#))
         (throw e#))
       (finally
         (when verbose#
           (prn [:final :blocking-jail
                 :timeout timeout#
                 :unblock-tag unblock-tag#
                 :finalization '~finalization]))
         ~finalization))))

(defmacro looping-jail
  "jail body by looping"
  [[stop-condition finalization verbose] & body]
  `(let [verbose# ~verbose]
     (try
       (loop []
         ~@body
         (when-not ~stop-condition
           (recur)))
       (catch Exception e#
         (when verbose#
           (clojure.stacktrace/print-stack-trace e#))
         (throw e#))
       (finally
         (when verbose#
           (prn [:final *current-plugin* :looping-jail
                 :stop-condition '~stop-condition
                 :finalization '~finalization]))
         ~finalization))))

(defn register-exit-hook
  "register exit hook on the main thread"
  [key hook]
  (swap! exit-hooks assoc key hook))

(defn unregister-exit-hook
  "undo register-exit-hook"
  [key]
  (swap! exit-hooks dissoc key))

(defn execute-all-exit-hooks
  "execute all registered exit hooks"
  []
  (doseq [[_ hook] @exit-hooks]
    (hook)))
