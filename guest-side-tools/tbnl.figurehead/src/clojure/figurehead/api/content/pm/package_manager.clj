;;; https://github.com/android/platform_frameworks_base/blob/android-4.3_r3.1/cmds/pm/src/com/android/commands/pm/Pm.java
(ns figurehead.api.content.pm.package-manager
  "pm (Package Manager) wrapper"
  (:require (core [state :as state :refer [defcommand]]))  
  (:require (figurehead.util [services :as services :refer [get-service]]))
  (:require [figurehead.api.content.pm.package-manager-parser :as parser])
  (:require [clojure.string :as str]
            [clojure.java.io :as io])  
  (:import (android.app IActivityManager)
           (android.content ComponentName)
           (android.content.pm ActivityInfo
                               ApplicationInfo
                               ContainerEncryptionParams
                               FeatureInfo
                               IPackageDataObserver
                               IPackageDataObserver$Stub
                               IPackageDeleteObserver
                               IPackageDeleteObserver$Stub
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
           (android.util Base64)
           (com.android.internal.content PackageHelper)
           (java.io File)
           (javax.crypto SecretKey)
           (javax.crypto.spec IvParameterSpec
                              SecretKeySpec)
           (org.apache.commons.io FileUtils)))

(declare get-raw-packages get-packages get-all-package-names get-package-components
         get-raw-features get-features
         get-raw-libraries get-libraries
         get-raw-instrumentations get-instrumentations
         get-raw-permission-groups get-permissions-by-group

         get-install-location set-install-location
         push-file pull-file
         make-package-install-observer install-package
         make-package-delete-observer uninstall-package
         make-package-data-observer clear-package-data)

(defcommand get-raw-packages
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

(defcommand get-packages
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

(defcommand get-all-package-names
  "get list of package names"
  [{:keys []
    :as args}]
  (let [packages (get-raw-packages {})
        result (atom #{})]
    (doseq [^PackageInfo package packages]
      (swap! result conj
             (keyword (.packageName package))))
    @result))

(defcommand get-package-components
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


(defcommand get-raw-features
  "get all features on this device"
  [{:keys []
    :as args}]
  (let [^IPackageManager package-manager (get-service :package-manager)]
    (let [features  (.. package-manager
                        getSystemAvailableFeatures)]
      (seq features))))

(defcommand get-features
  "get all features on this device"
  [{:keys [brief?]
    :as args}]
  (let [features (get-raw-features {})
        result (atom {})]
    (doseq [^FeatureInfo feature features]
      (let [feature (parser/parse-feature-info feature)]
        (swap! result assoc
               (keyword (:name feature))
               (when-not brief?
                 feature))))
    @result))


(defcommand get-raw-libraries
  "get all libraries on this device"
  [{:keys []
    :as args}]
  (let [^IPackageManager package-manager (get-service :package-manager)]
    (let [libraries  (.. package-manager
                        getSystemSharedLibraryNames)]
      (seq libraries))))

(defcommand get-libraries
  "get all libraries on this device"
  [{:keys []
    :as args}]
  (let [libraries (get-raw-libraries {})
        result (atom [])]
    (doseq [^String library libraries]
      (swap! result conj
             library))
    @result))

(defcommand get-raw-instrumentations
  "get installed instrumentations on this device, optional for a specific package"
  [{:keys [^String package]
    :as args}]
  (let [^IPackageManager package-manager (get-service :package-manager)]
    (let [instrumentations  (.. package-manager
                                (queryInstrumentation package 0))]
      (seq instrumentations))))

(defcommand get-instrumentations
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

(defcommand get-raw-permission-groups
  "get permission groups"
  [{:keys []
    :as args}]
  (let [^IPackageManager package-manager (get-service :package-manager)]
    (let [permission-groups (.. package-manager
                                (getAllPermissionGroups 0))]
      (seq permission-groups))))

(defcommand get-permissions-by-group
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


(defcommand get-install-location
  "get install location"
  [{:keys []
    :as args}]
  (let [^IPackageManager package-manager (get-service :package-manager)
        location (.getInstallLocation package-manager)]
    (cond
     (= location PackageHelper/APP_INSTALL_AUTO) :auto
     (= location PackageHelper/APP_INSTALL_EXTERNAL) :external
     (= location PackageHelper/APP_INSTALL_INTERNAL) :internal
     :else location)))

(defcommand set-install-location
  "set install location"
  [{:keys [location]
    :or {location 0}
    :as args}]
  {:pre [(contains? #{PackageHelper/APP_INSTALL_AUTO
                      PackageHelper/APP_INSTALL_EXTERNAL
                      PackageHelper/APP_INSTALL_INTERNAL
                      :auto :internal :external} location)]}
  (let [location (cond
                  (contains? #{PackageHelper/APP_INSTALL_AUTO
                               PackageHelper/APP_INSTALL_EXTERNAL
                               PackageHelper/APP_INSTALL_INTERNAL}
                             location)
                  location

                  (contains? #{:auto :internal :external} location)
                  ({:auto PackageHelper/APP_INSTALL_AUTO
                    :internal PackageHelper/APP_INSTALL_INTERNAL
                    :external PackageHelper/APP_INSTALL_EXTERNAL} location))]
    (when location
      (let [^IPackageManager package-manager (get-service :package-manager)]
        (.setInstallLocation package-manager location)))))

(defcommand push-file
  "push file to device"
  [{:keys [^String content-in-base64
           file-name]
    :as args}]
  (when (and content-in-base64 file-name)
    (with-open [the-file (io/output-stream (io/file file-name))]
      (.write the-file ^bytes (Base64/decode content-in-base64
                                             Base64/DEFAULT)))))

(defcommand pull-file
  "pull file from device"
  [{:keys [file-name]
    :as args}]
  (when (and file-name)
    (Base64/encodeToString
     (FileUtils/readFileToByteArray (io/file file-name))
     (bit-or Base64/NO_WRAP
             0))))

(defcommand make-package-install-observer
  "make instance of IPackageInstallObserver$Stub"
  [{:keys [package-installed]
    :as args}]
  (proxy
      [IPackageInstallObserver$Stub]
      []
    (packageInstalled [package-name status]
      (locking this
        (when package-installed
          (package-installed package-name status))))))

(defcommand install-package
  "install package"
  [{:keys [apk-file-name
           package-name
           forward-lock?
           replace-existing?
           allow-test?
           external?
           internal?
           allow-downgrade?]
    :as args}]
  (when (and apk-file-name package-name)
    (let [^IPackageManager package-manager (get-service :package-manager)
          flags (atom 0)
          apk-uri (Uri/fromFile (io/file apk-file-name))]
      (when (and apk-uri)
        (when forward-lock?
          (swap! flags bit-or PackageManager/INSTALL_FORWARD_LOCK))
        (when replace-existing?
          (swap! flags bit-or PackageManager/INSTALL_REPLACE_EXISTING))
        (when allow-test?
          (swap! flags bit-or PackageManager/INSTALL_ALLOW_TEST))
        (when external?
          (swap! flags bit-or PackageManager/INSTALL_EXTERNAL))
        (when internal?
          (swap! flags bit-or PackageManager/INSTALL_INTERNAL))
        (when allow-downgrade?
          (swap! flags bit-or PackageManager/INSTALL_ALLOW_DOWNGRADE))
        (let [finished? (promise)
              result (atom 0)]
          (.installPackage package-manager
                           apk-uri
                           (make-package-install-observer
                            {:package-installed (fn [package-name status]
                                                  (reset! result status)
                                                  (deliver finished? true))})
                           @flags
                           package-name)
          @finished?
          @result)))))

(defcommand make-package-delete-observer
  "make instance of IPackageDeleteObserver$Stub"
  [{:keys [package-deleted]
    :as args}]
  (proxy
      [IPackageDeleteObserver$Stub]
      []
    (packageDeleted [package-name return-code]
      (locking this
        (when package-deleted
          (package-deleted package-name return-code))))))

(defcommand uninstall-package
  "uninstall package"
  [{:keys [package
           keep-data?]
    :or {keep-data? true}
    :as args}]
  (when package
    (let [^IPackageManager package-manager (get-service :package-manager)
          flags (atom PackageManager/DELETE_ALL_USERS)
          finished? (promise)
          successful? (atom false)]
      (when keep-data?
        (swap! flags bit-or PackageManager/DELETE_KEEP_DATA))
      (.deletePackageAsUser package-manager
                            package
                            (make-package-delete-observer
                             {:package-deleted
                              (fn [package-name return-code]
                                (reset! successful?
                                        (= return-code PackageManager/DELETE_SUCCEEDED))
                                (deliver finished? true))})
                            UserHandle/USER_OWNER
                            @flags)
      @finished?
      @successful?)))

(defcommand make-package-data-observer
  "make instance of IPackageDataObserver$Stub"
  [{:keys [on-remove-completed]
    :as args}]
  (proxy
      [IPackageDataObserver$Stub]
      []
    (onRemoveCompleted [package-name succeeded]
      (locking this
        (when on-remove-completed
          (on-remove-completed package-name succeeded))))))

(defcommand clear-package-data
  "clear package data"
  [{:keys [package]
    :as args}]
  (when package
    (let [^IActivityManager activity-manager (get-service :activity-manager)
          finished? (promise)
          successful? (atom false)]
      (.clearApplicationUserData activity-manager
                                 package
                                 (make-package-data-observer
                                  {:on-remove-completed
                                   (fn [package-name succeeded]
                                     (reset! successful?
                                             succeeded)
                                     (deliver finished? true))})
                                 UserHandle/USER_OWNER)
      @finished?
      @successful?)))


