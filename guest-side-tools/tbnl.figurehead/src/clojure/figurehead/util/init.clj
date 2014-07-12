(ns figurehead.util.init
  (:require (figurehead.util [services
                              :as services
                              :refer [register-service]]))
  (:import (android.os ServiceManager)
           (android.app ActivityManagerNative)
           (android.content.pm IPackageManager
                               IPackageManager$Stub)))

(declare init)

(defn- register-services
  "register services"
  []
  (register-service :activity-manager
                    #(ActivityManagerNative/getDefault))
  (register-service :package-manager
                    #(-> 
                      (ServiceManager/getService "package") 
                      (IPackageManager$Stub/asInterface))))

(defn init
  "initialization"
  []
  (register-services))

