(ns core.main
  (:require (core [init :as init]
                  [plugin :as plugin]
                  [state :as state]))
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.stacktrace :refer [print-stack-trace]]
            [clojure.core.async :as async :refer [chan <!!]])
  (import (java.util UUID)))

(defn main
  "the main entry"
  [& args]
  (try
    (state/add-state :instance-id
                     (-> (UUID/randomUUID) str keyword))
    ;; load the plugins and populate the parse-opts vector
    (let [{:keys [options arguments errors summary]}
          (parse-opts args (init/get-parse-opts-vector))

          verbose (:verbose options)]
      (let [plugins (:plugin options)]
        (when verbose
          (prn (list :load-plugins
                     plugins)))
        (doseq [plugin plugins]
          (plugin/load-plugin plugin))
        (doseq [plugin plugins]
          (when verbose
            (prn (list :populate-parse-opts-vector
                       plugin
                       (init/get-parse-opts-vector))))
          (plugin/populate-parse-opts-vector plugin
                                             (init/get-parse-opts-vector)))))

    ;; parse-opts again; initilize and run the plugins
    (let [{:keys [options arguments errors summary]}
          (parse-opts args (init/get-parse-opts-vector))

          verbose (:verbose options)]
      (reset! plugin/current-options options)
      (cond
       ;; ask for help
       (:help options)
       (do
         (println summary))

       :main
       (let [plugins (plugin/list-all-plugins-by-priority)]
         (doseq [plugin plugins]
           (plugin/init-and-run-plugin plugin options))
         (when-not (:batch options)
           ;; block the main Thread 
           (when verbose
             (prn {:batch (:batch options)}))
           (<!! (chan))))))

    (catch Throwable e
      (print-stack-trace e))
    (finally
      ;; http://clojuredocs.org/clojure_core/clojure.java.shell/sh#example_896
      (shutdown-agents))))
