(ns figurehead.api.content.pm.package-info
  (:require (figurehead.util [services :as services :refer [get-service]]))
  (:require [clojure.string :as str])
  (:import
   (android.content.pm IPackageManager
                       PackageManager
                       
                       ActivityInfo
                       ServiceInfo
                       ProviderInfo
                       
                       PackageInfo)))

(defn get-all-packages 
  "get all packages on this device"
  [{:keys []
    :as args}]
  (let [package-manager ^IPackageManager (get-service :package-manager)]
    (let [packages  (.. package-manager
                        (getInstalledPackages 0 0)
                        (getList))]
      (for [^PackageInfo package-info packages]
        package-info))))

(defn get-package-info 
  "get app components for a specific package"
  [{:keys [package]
    :as args}]
  (when package
    (let [package-manager ^IPackageManager (get-service :package-manager)]
      (when-let [pkg-info (.getPackageInfo package-manager
                                           (cond
                                             (keyword? package)
                                             (name package)

                                             (sequential? package)
                                             (str/join "."
                                                       (map #(cond (keyword? %) (name %)
                                                                   :else (str %))
                                                            package))

                                             :else
                                             (str package))
                                           (bit-or PackageManager/GET_ACTIVITIES
                                                   PackageManager/GET_PROVIDERS
                                                   PackageManager/GET_RECEIVERS
                                                   PackageManager/GET_SERVICES
                                                   PackageManager/GET_PERMISSIONS
                                                   PackageManager/GET_CONFIGURATIONS)
                                           0)]
        {:activities (for [^ActivityInfo activity (.activities pkg-info)]
                       {:name (.name activity)})
         :services (for [^ServiceInfo service (.services pkg-info)]
                     {:name (.name service)})
         :providers (for [^ProviderInfo provider (.providers pkg-info)]
                      {:name (.name provider)})
         :receivers (for [^ActivityInfo receiver (.receivers pkg-info)]
                      {:name (.name receiver)})}))))
