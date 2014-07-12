(ns figurehead.util.services
  (:require (core [bus :as bus])))

(declare policy services
         get-service list-service register-service)

(def policy
  "internal policy of service procedures"
  (atom
   {:register-retry-interval 500}))

(def services
  "map from service tag to its (delayed) instance"
  (atom {}))

(defn get-service 
  "get service by tag"
  [tag]
  (when-let [service (tag @services)]
    ;; get the (delayed) service
    @service))

(defn list-services 
  "get tags of all services"
  []
  (keys @services))

(defn register-service
  "register a service with tag by (obtain-fn)"
  [tag obtain-fn]
  (swap! services assoc tag
         (delay
          (loop [h (obtain-fn)]
            (if h
              h
              (do
                (bus/say!! :error (str "obtaining " (name tag) "..."))
                (Thread/sleep (:register-retry-interval @policy))
                (recur (obtain-fn))))))))

