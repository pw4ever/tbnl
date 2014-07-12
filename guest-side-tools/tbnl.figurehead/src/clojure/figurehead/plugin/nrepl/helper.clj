;;; acknowledgement: https://github.com/clojure-android/neko
(ns figurehead.plugin.nrepl.helper
  (:require (core [plugin :as plugin]))
  (:require clojure.tools.nrepl.server
            clojure.tools.nrepl.middleware.interruptible-eval)
  (:import java.io.File
           java.util.concurrent.atomic.AtomicLong
           java.util.concurrent.ThreadFactory))

(declare enable-dynamic-compilation
         start-repl)

(def defaults
  (atom
   {
    :repl-worker-thread-stack-size 1048576     ; nrepl 1 M
    }))

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
    (.mkdir (clojure.java.io/file path))
    (plugin/set-state-entry :repl-dynamic-compilation-path
                            path)
    (System/setProperty "clojure.compile.path" path)
    (alter-var-root #'clojure.core/*compile-path*
                    (constantly path))))

(defn start-repl
  "neko.init/start-repl"
  [& repl-args]
  (binding [*ns* (create-ns 'user)]
    (refer-clojure)
    (use 'clojure.tools.nrepl.server)
    (require '[clojure.tools.nrepl.middleware.interruptible-eval :as ie])
    (with-redefs-fn {(resolve 'ie/configure-thread-factory)
                     android-thread-factory}
      #(apply (resolve 'start-server) repl-args))))
