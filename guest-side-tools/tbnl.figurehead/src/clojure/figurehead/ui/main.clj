(ns figurehead.ui.main
  (:require (neko [activity :refer [defactivity set-content-view!]]
                  [notify :refer [toast]]
                  [ui :refer [make-ui]]
                  [threading :refer [on-ui]]
                  [init]
                  [compilation]))
  (:require [neko.activity :refer [defactivity set-content-view!]]
            [neko.notify :refer [toast]]
            [neko.ui :refer [make-ui]]
            [neko.threading :refer [on-ui]]
            [neko.init]
            [neko.compilation])
  (:require [clojure.core.async :refer [thread]])
  (:require (cider.nrepl.middleware apropos
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
  (:import (android.app Activity)))

(def first-time? (atom true))
(def defaults
  (atom {:repl-port 9999}))

(defactivity figurehead.ui.main
  :on-create
  (fn [^Activity this bundle]
    (when @first-time?
      (thread
        (reset! first-time? false)
        (neko.init/init (.getApplication this)
                        :port (:repl-port @defaults))))
    (on-ui
     (set-content-view! this
                        (make-ui [:linear-layout {:orientation :vertical
                                                  :layout-width :fill
                                                  :layout-height :wrap}
                                  [:edit-text {:layout-width :fill}]])))))
