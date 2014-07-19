(ns mastermind.plugin.model.figurehead.helper
  (:require (core [init :as init]
                  [state :as state]
                  [bus :as bus]
                  [plugin :as plugin]))
  (:require
   [clojure.core.async :as async :refer [chan 
                                         close!
                                         go
                                         >! <!]]
   [clojure.java.io :as io]
   ))

(defn create-input-chans [traces]
  "create async channels from trace sources"
  (let [input-chans (atom {})]
    (doseq [trace traces]
      (let [c (chan)
            session (keyword (gensym "input-chans"))]
        (swap! input-chans assoc session c)
        (go
          (with-open [rdr (io/reader trace)]
            (loop [news (.readLine rdr)]
              (when news
                (try
                  (let [news (read-string news)]
                    (>! c news))
                  (catch RuntimeException e
                    ;; read-string error
                    ))
                (recur (.readLine rdr)))))
          ;; need to remove itself from input-chans
          (swap! input-chans dissoc session)
          (close! c))))
    input-chans))
