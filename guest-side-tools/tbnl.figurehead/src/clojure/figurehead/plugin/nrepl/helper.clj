;;; acknowledgement: https://github.com/clojure-android/neko
(ns figurehead.plugin.nrepl.helper
  "helper for starting nREPL server"
  (:require (core [plugin :as plugin]))
  (:require [clojure.java.io :refer [file delete-file]]
            [clojure.tools.nrepl.server :as nrepl-server]
            clojure.tools.nrepl.middleware.interruptible-eval)
  (:import java.io.File
           java.util.concurrent.atomic.AtomicLong
           java.util.concurrent.ThreadFactory))

(declare enable-dynamic-compilation
         clean-cache
         start-repl)

(def defaults
  (atom
   {
    :repl-worker-thread-stack-size 8388608     ; nrepl 8M
    }))

(def ^{:doc "ref: neko.compilation"
       :private true}
  cache-path (atom nil))

(defn- android-thread-factory
  []
  (let [counter (AtomicLong. 0)]
    (reify ThreadFactory
      (newThread [_ runnable]
        (doto (Thread. (.getThreadGroup (Thread/currentThread))
                       runnable
                       (format "nREPL-worker-%s" (.getAndIncrement counter))
                       (:repl-worker-thread-stack-size @defaults))
          (.setDaemon true))))))

(defn- get-absolute-path-from-cwd
  "get absolute path of name"
  [& path-components]
  (clojure.string/join File/separator
                       ;; http://developer.android.com/reference/java/lang/System.html
                       (into [(System/getProperty "java.io.tmpdir")]
                             path-components)))


(defn enable-dynamic-compilation
  "enable dynamic compilation; adapt from neko.compilation/init"
  [clojure-cache-dir]
  (let [path (get-absolute-path-from-cwd clojure-cache-dir)]
    (.mkdir (file path))
    (plugin/set-state-entry :repl-dynamic-compilation-path
                            path)
    (reset! cache-path path)
    (System/setProperty "clojure.compile.path" path)
    (alter-var-root #'clojure.core/*compile-path*
                    (constantly path))
    ;; clean staled cache
    (clean-cache)))

(defn- cache-file?
  "from neko.compilation"
  [^String name]
  (and (.startsWith name "repl-")
       (or (.endsWith name ".dex")
           (.endsWith name ".jar"))))

(defn clean-cache
  "clean compilation cache"
  []
  (locking cache-path
    (when-let [cache-path @cache-path]
      (doseq [^File f (file-seq (file cache-path))]
        (try
          (when (and (.isFile f)
                     (cache-file? (.getName f)))
            ;; wierd EBUSY error: http://stackoverflow.com/a/11776458
            (let [^File tmp (file (str (.getAbsolutePath f)
                                       (System/currentTimeMillis)))]
              (.renameTo f tmp)
              (delete-file tmp)))
          (catch Exception e)))
      ;; remake the directory if necessary
      (let [^File compile-path (file cache-path)]
        (when-not (.exists compile-path)
          (.mkdir compile-path))))))

(defn start-repl
  "neko.init/start-repl"
  [& repl-args]
  (binding [*ns* (create-ns 'user)]
    (refer-clojure)

    (use 'clojure.repl)
    (use 'clojure.pprint)
    (use 'clojure.java.io)
    (require '(clojure [string :as str]
                       [set :as set]))

    ;; Android API wrapper
    (require '[figurehead.api.app.activity-manager :as activity-manager])
    (require '[figurehead.api.content.intent :as intent])
    (require '[figurehead.api.content.pm.package-manager :as package-manager])
    (require '[figurehead.api.content.pm.package-manager-parser :as package-manager-parser])
    (require '[figurehead.api.view.input :as input])
    (require '[figurehead.api.os.user-manager.user-manager :as user-manager])
    (require '[figurehead.api.os.user-manager.user-manager-parser :as user-manager-parser])
    (require '[figurehead.api.os.util :as os-util])
    (require '[figurehead.api.util.file :as util-file])

    (require '(core [bus :as bus]
                    [plugin :as plugin]
                    [state :as state]))

    (use 'clojure.tools.nrepl.server)
    (require '[clojure.tools.nrepl.middleware.interruptible-eval :as ie])
    (with-redefs-fn {(resolve 'ie/configure-thread-factory)
                     android-thread-factory}
      #(apply (resolve 'start-server) repl-args))))
