;;; https://github.com/android/platform_frameworks_base/blob/android-4.3_r3.1/cmds/pm/src/com/android/commands/pm/Pm.java
(ns figurehead.api.content.pm.package-manager
  (:require (figurehead.util [services :as services :refer [get-service]]))
  (:require [figurehead.api.content.pm.package-manager-parser :as parser])
  (:require [clojure.string :as str])  
  (:import (android.content ComponentName)
           (android.content.pm ActivityInfo
                               ApplicationInfo
                               ContainerEncryptionParams
                               FeatureInfo
                               IPackageDataObserver
                               IPackageDeleteObserver
                               IPackageInstallObserver
                               IPackageInstallObserver$Stub
                               IPackageManager
                               InstrumentationInfo
                               PackageInfo
                               PackageItemInfo
                               PackageManager
                               ParceledListSlice
                               PermissionGroupInfo
                               PermissionInfo
                               ProviderInfo
                               ServiceInfo
                               UserInfo
                               VerificationParams)
           (android.content.res AssetManager
                                Resources)
           (android.net Uri)
           (android.os IUserManager
                       RemoteException
                       ServiceManager
                       UserHandle
                       UserManager)
           (javax.crypto SecretKey)
           (javax.crypto.spec IvParameterSpec
                              SecretKeySpec)))

(declare get-raw-packages get-packages get-all-package-names get-package-components
         get-raw-features get-features
         get-raw-libraries get-libraries
         get-raw-instrumentations get-instrumentations
         get-raw-permission-groups get-permissions-by-group)

(defn get-raw-packages
  "get all packages on this device"
  [{:keys []
    :as args}]
  (let [^IPackageManager package-manager (get-service :package-manager)]
    (let [packages  (.. package-manager
                        (getInstalledPackages
                         (bit-or PackageManager/GET_ACTIVITIES
                                 PackageManager/GET_CONFIGURATIONS
                                 PackageManager/GET_DISABLED_COMPONENTS
                                 PackageManager/GET_DISABLED_UNTIL_USED_COMPONENTS
                                 PackageManager/GET_GIDS
                                 PackageManager/GET_INSTRUMENTATION
                                 PackageManager/GET_INTENT_FILTERS
                                 PackageManager/GET_PERMISSIONS
                                 PackageManager/GET_PROVIDERS
                                 PackageManager/GET_RECEIVERS
                                 PackageManager/GET_SERVICES
                                 PackageManager/GET_SIGNATURES)
                         0)
                        getList)]
      (seq packages))))

(defn get-packages
  "get all packages on this device"
  [{:keys [brief?]
    :as args}]
  (let [packages (get-raw-packages {})
        result (atom {})]
    (doseq [^PackageInfo package packages]
      (let [package (parser/parse-package-info package)]
        (swap! result assoc
               (keyword (:package-name package))
               (when-not brief?
                 package))))
    @result))

(defn get-all-package-names
  "get list of package names"
  [{:keys []
    :as args}]
  (let [packages (get-raw-packages {})
        result (atom #{})]
    (doseq [^PackageInfo package packages]
      (swap! result conj
             (keyword (.packageName package))))
    @result))

(defn get-package-components
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
                                                   PackageManager/GET_PERMISSIONS)
                                           0)]
        {:activities (set (for [^ActivityInfo activity (.activities pkg-info)]
                            (keyword (.name activity))))
         :services (set (for [^ServiceInfo service (.services pkg-info)]
                          (keyword (.name service))))
         :providers (set (for [^ProviderInfo provider (.providers pkg-info)]
                           (keyword (.name provider))))
         :receivers (set (for [^ActivityInfo receiver (.receivers pkg-info)]
                           (keyword (.name receiver))))
         :permissions (set (for [^PermissionInfo permission (.permissions pkg-info)]
                             (keyword (.name permission))))}))))


(defn get-raw-features
  "get all features on this device"
  [{:keys []
    :as args}]
  (let [^IPackageManager package-manager (get-service :package-manager)]
    (let [features  (.. package-manager
                        getSystemAvailableFeatures)]
      (seq features))))

(defn get-features
  "get all features on this device"
  [{:keys [brief?]
    :as args}]
  (let [features (get-raw-features {})
        result (atom {})]
    (doseq [^FeatureInfo feature features]
      (let [feature (parser/parse-feature-info)]
        (swap! result assoc
               (keyword (:name feature))
               (when-not brief?
                 feature))))
    @result))


(defn get-raw-libraries
  "get all libraries on this device"
  [{:keys []
    :as args}]
  (let [^IPackageManager package-manager (get-service :package-manager)]
    (let [libraries  (.. package-manager
                        getSystemSharedLibraryNames)]
      (seq libraries))))

(defn get-libraries
  "get all libraries on this device"
  [{:keys []
    :as args}]
  (let [libraries (get-raw-libraries {})
        result (atom [])]
    (doseq [^String library libraries]
      (swap! result conj
             library))
    @result))

(defn get-raw-instrumentations
  "get installed instrumentations on this device, optional for a specific package"
  [{:keys [^String package]
    :as args}]
  (let [^IPackageManager package-manager (get-service :package-manager)]
    (let [instrumentations  (.. package-manager
                                (queryInstrumentation package 0))]
      (seq instrumentations))))

(defn get-instrumentations
  "get installed instrumentations on this device, optional for a specific package"
  [{:keys [^String package
           brief?]
    :as args}]
  (let [instrumentations (get-raw-instrumentations {:package package})
        result (atom {})]
    (doseq [^InstrumentationInfo instrumentation instrumentations]
      (let [instrumentation (parser/parse-instrumentation-info instrumentation)]
        (swap! result assoc
               (keyword (:name instrumentation))
               (when-not brief?
                 instrumentation))))
    @result))

(defn get-raw-permission-groups
  "get permission groups"
  [{:keys []
    :as args}]
  (let [^IPackageManager package-manager (get-service :package-manager)]
    (let [permission-groups (.. package-manager
                                (getAllPermissionGroups 0))]
      (seq permission-groups))))

(defn get-permissions-by-group
  "get all permissions by group"
  [{:keys [brief?]
    :as args}]
  (let [permission-groups (get-raw-permission-groups {})
        result (atom {})]
    (doseq [^PermissionGroupInfo permission-group permission-groups]
      (let [permission-group (parser/parse-permission-group-info permission-group)]
        (swap! result assoc
               (keyword (:name permission-group))
               (merge {}
                      (when-not brief?
                        permission-group)
                      {:permissions
                       (let [^IPackageManager package-manager (get-service :package-manager)
                             result (atom {})]
                         (doseq [^PermissionInfo permission
                                 (.queryPermissionsByGroup package-manager
                                                           (:name permission-group)
                                                           0)]
                           (let [permission (parser/parse-permission-info permission)]
                             (swap! result assoc
                                    (keyword (:name permission))
                                    (when-not brief?
                                      permission))))
                         @result)}))))
    @result))


