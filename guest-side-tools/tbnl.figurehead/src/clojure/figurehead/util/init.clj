(ns figurehead.util.init
  (:require (figurehead.util [services
                              :as services
                              :refer [register-service]]))
  (:import (android.os ServiceManager
                       IUserManager
                       IUserManager$Stub)
           (android.app ActivityManagerNative
                        IActivityManager)
           (android.content.pm IPackageManager
                               IPackageManager$Stub)))

(declare init)

(defn- register-services
  "register services"
  []
  (register-service :activity-manager
                    ^IActivityManager #(ActivityManagerNative/getDefault))
  (register-service :user-manager
                    ^IUserManager #(->>
                                    (ServiceManager/getService "user")
                                    (IUserManager$Stub/asInterface)))
  (register-service :package-manager
                    ^IPackageManager #(->> 
                                       (ServiceManager/getService "package") 
                                       (IPackageManager$Stub/asInterface))))

(defn init
  "initialization"
  []
  (register-services))

