(ns core.init
  (:require
   (core [state :as state]
         [plugin :as plugin])))

(declare add-to-parse-opts-vector get-parse-opts-vector
         parse-opts-vector-helper
         set-default-plugins)

(def ^:private parse-opts-vector
  "the vector to feed into clojure.tools.cli/parse-opts"
  (atom [
         ["-h" "--help" "show help"]         
         ["-v" "--verbose" "be verbose and show debug info"]
         ["-B" "--batch" "batch mode: no block after loading plugins"]
         ]))

(defn add-to-parse-opts-vector
  "lines are added into the parse-opts vector"
  [lines]
  (swap! parse-opts-vector into lines))

(defn get-parse-opts-vector
  "return the parse-opts vector"
  []
  @parse-opts-vector)

(def parse-opts-vector-helper
  "help parse the parse-opts vector"
  (atom {
         :parse-fn
         {
          :inet-port (comp #(when (and (> % 0)
                                       (< % 65536))
                              %)
                           #(Integer/parseInt %))
          :file #(clojure.java.io/file %)
          }
         }))

(defn set-default-plugins
  [& plugins]
  (swap! parse-opts-vector
         conj
         (let [option :plugin]
           ["-P"
            (str "--"
                 (name option)
                 " [PLUGIN]")
            "[m] plugin to load"
            :default plugins
            :parse-fn symbol
            :assoc-fn (fn [m k v]
                        (update-in m [k]
                                   (comp vec conj) v))])))

(defmacro require-and-set-default-plugins
  "set default plugin namespaces to be required/loaded"
  [& plugins]
  ;; compile-time require will put the plugin on classpath
  (doseq [plugin plugins]
    (require (plugin/get-plugin-main-entry plugin)))
  `(set-default-plugins ~@(map (fn [plugin] `'~plugin) plugins)))
