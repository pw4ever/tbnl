(ns figurehead.api.content.pm.package-manager-parser
  "parse Package Manager objects into Clojure data structures"
  (:require (figurehead.util [services :as services :refer [get-service]]))
  (:import (android.content ComponentName)
           (android.content.pm ActivityInfo
                               ApplicationInfo
                               ComponentInfo
                               ConfigurationInfo
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
           (android.graphics.drawable Drawable)
           (java.util WeakHashMap)
           (javax.crypto SecretKey)
           (javax.crypto.spec IvParameterSpec
                              SecretKeySpec)))

(declare resource-cache get-resources-by-package-name get-resources
         load-res-string load-res-resource-name load-res-drawable
         parse-package-info

         parse-package-item-info
         parse-component-info

         parse-activity-info
         parse-application-info
         parse-configuration-info
         parse-instrumentation-info
         parse-permission-group-info
         parse-permission-info
         parse-provider-info
         parse-feature-info
         parse-service-info)

(def resource-cache
  "resource cache {package-name resources}"
  (WeakHashMap.))

(defn ^Resources get-resources-by-package-name
  "get resources by package name"
  [^String package-name]
  (if-let [res (.get ^WeakHashMap resource-cache package-name)]
    res
    (let [^IPackageManager package-manager (get-service :package-manager)
          ^ApplicationInfo application-info (.getApplicationInfo package-manager
                                                                 package-name
                                                                 0 0)
          ^AssetManager asset-manager (AssetManager.)]
      (.addAssetPath asset-manager (.publicSourceDir application-info))
      (let [res (Resources. asset-manager nil nil)]
        (.put ^WeakHashMap resource-cache package-name res)
        res))))

(defn ^Resources get-resources
  "get resources"
  [^PackageItemInfo package-item-info]
  (let [package-name (.packageName package-item-info)]
    (get-resources-by-package-name package-name)))

(defn ^String load-res-string
  "load string from resource"
  [^PackageItemInfo package-item res non-localized]
  (let [resource (get-resources package-item)]
    (cond non-localized (str non-localized)
          (and resource
               res
               (not= res 0))
          (.getString resource res))))

(defn ^String load-res-resource-name
  "load resource name from resource"
  [^PackageItemInfo package-item res]
  (let [resource (get-resources package-item)]
    (when (and resource
               res
               (not= res 0))
      (.getResourceName resource res))))

(defn ^Drawable load-res-drawable
  "load drawable from resource"
  [^PackageItemInfo package-item res]
  (let [resource (get-resources package-item)]
    (when (and resource
               res
               (not= res 0))
      (.getDrawable resource res))))

(defn parse-package-info
  "parse PackageInfo"
  [^PackageInfo package]
  (merge {}
         {:activities (set (map parse-activity-info
                                (.activities package)))
          :application-info (parse-application-info (.applicationInfo package))
          :config-preferences (set (map parse-configuration-info
                                        (.configPreferences package)))
          :first-install-time (.firstInstallTime package)
          :gids (set (.gids package))
          :install-location (#(case %
                                PackageInfo/INSTALL_LOCATION_AUTO
                                :auto
                                PackageInfo/INSTALL_LOCATION_INTERNAL_ONLY
                                :internal-only
                                PackageInfo/INSTALL_LOCATION_PREFER_EXTERNAL
                                :prefer-external
                                PackageInfo/INSTALL_LOCATION_UNSPECIFIED
                                :unspecified
                                %)
                             (.installLocation package))
          :instrumentation (set (map parse-instrumentation-info
                                     (.instrumentation package)))
          :last-update-time (.lastUpdateTime package)
          :package-name (.packageName package)
          :permissions (set (map parse-permission-info
                                 (.permissions package)))
          :providers (set (map parse-provider-info
                               (.providers package)))
          :receivers (set (map parse-activity-info
                               (.receivers package)))
          :req-features (set (map parse-feature-info
                                  (.reqFeatures package)))
          :requested-permissions (zipmap (vec (.requestedPermissions package))
                                         (vec (map #(let [flags (atom #{})]
                                                      (when (bit-and %
                                                                     PackageInfo/REQUESTED_PERMISSION_GRANTED)
                                                        (swap! flags conj :granted))
                                                      (when (bit-and %
                                                                     PackageInfo/REQUESTED_PERMISSION_REQUIRED)
                                                        (swap! flags conj :required))
                                                      @flags)
                                                   (.requestedPermissionsFlags package))))
          :required-account-type (.requiredAccountType package)
          :required-for-all-users (.requiredForAllUsers package)
          :restricted-account-type (.restrictedAccountType package)
          :services (set (map parse-service-info
                              (.services package)))
          :shared-user-id (.sharedUserId package)
          :shared-user-label (.sharedUserLabel package)
          :signatures (set (.signatures package))
          :version-code (.versionCode package)
          :version-name (.versionName package)}))

(defn parse-package-item-info
  "parse PackageItemInfo"
  [^PackageItemInfo package-item]
  (merge {}
         {:icon (load-res-drawable package-item
                               (.icon package-item))
          :label (load-res-string package-item
                              (.labelRes package-item)
                              (.nonLocalizedLabel package-item))
          :label-res (.labelRes package-item)
          :logo (load-res-drawable package-item
                               (.logo package-item))
          :meta-data (.metaData package-item)
          :name (.name package-item)
          :non-localized-label (str (.nonLocalizedLabel package-item))
          :package-name (.packageName package-item)}))

(defn parse-component-info
  "parse ComponentInfo"
  [^ComponentInfo component]
  (merge {}
         (parse-package-item-info component)
         {:application-info (parse-application-info (.applicationInfo component))
          :description  (load-res-string component
                                     (.descriptionRes component)
                                     nil)
          :description-res (.descriptionRes component)
          :enabled (.enabled component)
          :exported (.exported component)
          :process-name (.processName component)}))

(defn parse-activity-info
  "parse ActivityInfo"
  [^ActivityInfo activity]
  (merge {}
         (parse-component-info activity)
         {:config-changes (#(let [flags (atom #{})]
                              (when (bit-and %
                                             ActivityInfo/CONFIG_DENSITY)
                                (swap! flags conj :density))
                              (when (bit-and %
                                             ActivityInfo/CONFIG_FONT_SCALE)
                                (swap! flags conj :font-scal))
                              (when (bit-and %
                                             ActivityInfo/CONFIG_KEYBOARD)
                                (swap! flags conj :keyboard))
                              (when (bit-and %
                                             ActivityInfo/CONFIG_KEYBOARD_HIDDEN)
                                (swap! flags conj :keyboard-hidden))
                              (when (bit-and %
                                             ActivityInfo/CONFIG_LAYOUT_DIRECTION)
                                (swap! flags conj :layout-direction))
                              (when (bit-and %
                                             ActivityInfo/CONFIG_LOCALE)
                                (swap! flags conj :locale))
                              (when (bit-and %
                                             ActivityInfo/CONFIG_MCC)
                                (swap! flags conj :mcc))
                              (when (bit-and %
                                             ActivityInfo/CONFIG_MNC)
                                (swap! flags conj :mnc))
                              (when (bit-and %
                                             ActivityInfo/CONFIG_NAVIGATION)
                                (swap! flags conj :navigation))
                              (when (bit-and %
                                             ActivityInfo/CONFIG_ORIENTATION)
                                (swap! flags conj :orientation))
                              (when (bit-and %
                                             ActivityInfo/CONFIG_SCREEN_LAYOUT)
                                (swap! flags conj :screen-layout))
                              (when (bit-and %
                                             ActivityInfo/CONFIG_SCREEN_SIZE)
                                (swap! flags conj :screen-size))
                              (when (bit-and %
                                             ActivityInfo/CONFIG_SMALLEST_SCREEN_SIZE)
                                (swap! flags conj :smallest-screen-size))
                              (when (bit-and %
                                             ActivityInfo/CONFIG_TOUCHSCREEN)
                                (swap! flags conj :touchscreen))
                              (when (bit-and %
                                             ActivityInfo/CONFIG_UI_MODE)
                                (swap! flags conj :ui-mode))
                              @flags)
                           (.configChanges activity))
          :flags (#(let [flags (atom #{})]
                     (when (bit-and %
                                    ActivityInfo/FLAG_ALLOW_TASK_REPARENTING)
                       (swap! flags conj :allow-task-reparenting))
                     (when (bit-and %
                                    ActivityInfo/FLAG_ALWAYS_RETAIN_TASK_STATE)
                       (swap! flags conj :always-retain-task-state))
                     (when (bit-and %
                                    ActivityInfo/FLAG_CLEAR_TASK_ON_LAUNCH)
                       (swap! flags conj :clear-task-on-launch))
                     (when (bit-and %
                                    ActivityInfo/FLAG_EXCLUDE_FROM_RECENTS)
                       (swap! flags conj :exclude-from-recents))
                     (when (bit-and %
                                    ActivityInfo/FLAG_FINISH_ON_CLOSE_SYSTEM_DIALOGS)
                       (swap! flags conj :finish-on-close-system-dialogs))
                     (when (bit-and %
                                    ActivityInfo/FLAG_FINISH_ON_TASK_LAUNCH)
                       (swap! flags conj :finish-on-task-launch))
                     (when (bit-and %
                                    ActivityInfo/FLAG_HARDWARE_ACCELERATED)
                       (swap! flags conj :hardware-accelerated))
                     (when (bit-and %
                                    ActivityInfo/FLAG_IMMERSIVE)
                       (swap! flags conj :immersive))
                     (when (bit-and %
                                    ActivityInfo/FLAG_MULTIPROCESS)
                       (swap! flags conj :multiprocess))
                     (when (bit-and %
                                    ActivityInfo/FLAG_NO_HISTORY)
                       (swap! flags conj :no-history))
                     (when (bit-and %
                                    ActivityInfo/FLAG_PRIMARY_USER_ONLY)
                       (swap! flags conj :primary-user-only))
                     (when (bit-and %
                                    ActivityInfo/FLAG_SHOW_ON_LOCK_SCREEN)
                       (swap! flags conj :show-on-lock-screen))
                     (when (bit-and %
                                    ActivityInfo/FLAG_SINGLE_USER)
                       (swap! flags conj :single-user))
                     (when (bit-and %
                                    ActivityInfo/FLAG_STATE_NOT_NEEDED)
                       (swap! flags conj :state-not-needed))
                     @flags)
                  (.flags activity))
          :launch-mode (#(case %
                           ActivityInfo/LAUNCH_MULTIPLE
                           :multiple
                           ActivityInfo/LAUNCH_SINGLE_INSTANCE
                           :single-instance
                           ActivityInfo/LAUNCH_SINGLE_TASK
                           :single-task
                           ActivityInfo/LAUNCH_SINGLE_TOP
                           :single-top
                           %)
                        (.launchMode activity))
          :parent-activity-name (.parentActivityName activity)
          :permission (.permission activity)
          :screen-orientation (#(case %
                                  ActivityInfo/SCREEN_ORIENTATION_BEHIND
                                  :behind
                                  ActivityInfo/SCREEN_ORIENTATION_FULL_SENSOR
                                  :full-sensor
                                  ActivityInfo/SCREEN_ORIENTATION_FULL_USER
                                  :full-user
                                  ActivityInfo/SCREEN_ORIENTATION_LANDSCAPE
                                  :landscape
                                  ActivityInfo/SCREEN_ORIENTATION_LOCKED
                                  :locked
                                  ActivityInfo/SCREEN_ORIENTATION_NOSENSOR
                                  :nosensor
                                  ActivityInfo/SCREEN_ORIENTATION_PORTRAIT
                                  :portrait
                                  ActivityInfo/SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                                  :reverse-landscape
                                  ActivityInfo/SCREEN_ORIENTATION_REVERSE_PORTRAIT
                                  :reverse-potrait
                                  ActivityInfo/SCREEN_ORIENTATION_SENSOR
                                  :sensor
                                  ActivityInfo/SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                  :sensor-landscape
                                  ActivityInfo/SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                  :sensor-portrait
                                  ActivityInfo/SCREEN_ORIENTATION_UNSPECIFIED
                                  :unspecified
                                  ActivityInfo/SCREEN_ORIENTATION_USER
                                  :user
                                  ActivityInfo/SCREEN_ORIENTATION_USER_LANDSCAPE
                                  :user-landscape
                                  ActivityInfo/SCREEN_ORIENTATION_USER_PORTRAIT
                                  :user-portrait
                                  %)
                               (.screenOrientation activity))
          :soft-input-mode (.softInputMode activity)
          :target-activity (.targetActivity activity)
          :task-affinity (.taskAffinity activity)
          :theme (load-res-resource-name activity
                                     (.theme activity))
          :ui-options (#(case %
                          ActivityInfo/UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW
                          :split-action-bar-when-narrow
                          %)
                       (.uiOptions activity))}))

(defn parse-application-info
  "parse ApplicationInfo"
  [^ApplicationInfo application]
  (merge {}
         (parse-package-item-info application)
         {:backup-agent-name (.backupAgentName application)
          :class-name (.className application)
          :compatible-width-limit-dp (.compatibleWidthLimitDp application)
          :data-dir (.dataDir application)
          :description (load-res-string application
                                    (.descriptionRes application)
                                    nil)
          :description-res (.descriptionRes application)
          :enabled (.enabled application)
          :enabled-setting (.enabledSetting application)
          :flags (#(let [flags (atom #{})]
                     (when (bit-and %
                                    ApplicationInfo/FLAG_ALLOW_BACKUP)
                       (swap! flags conj :allow-backup))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_ALLOW_CLEAR_USER_DATA)
                       (swap! flags conj :allow-clear-user-data))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_ALLOW_TASK_REPARENTING)
                       (swap! flags conj :allow-task-reparenting))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_CANT_SAVE_STATE)
                       (swap! flags conj :cant-save-state))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_DEBUGGABLE)
                       (swap! flags conj :debuggable))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_EXTERNAL_STORAGE)
                       (swap! flags conj :external-storage))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_FACTORY_TEST)
                       (swap! flags conj :factory-test))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_FORWARD_LOCK)
                       (swap! flags conj :forward-lock))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_HAS_CODE)
                       (swap! flags conj :has-code))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_INSTALLED)
                       (swap! flags conj :installed))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_IS_DATA_ONLY)
                       (swap! flags conj :is-data-only))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_KILL_AFTER_RESTORE)
                       (swap! flags conj :kill-after-restore))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_LARGE_HEAP)
                       (swap! flags conj :large-heap))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_PERSISTENT)
                       (swap! flags conj :persistent))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_RESIZEABLE_FOR_SCREENS)
                       (swap! flags conj :resizeable-for-screens))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_RESTORE_ANY_VERSION)
                       (swap! flags conj :restore-any-version))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_STOPPED)
                       (swap! flags conj :stopped))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_SUPPORTS_LARGE_SCREENS)
                       (swap! flags conj :supports-large-screens))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_SUPPORTS_NORMAL_SCREENS)
                       (swap! flags conj :supports-normal-screens))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_SUPPORTS_RTL)
                       (swap! flags conj :supports-rtl))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_SUPPORTS_SCREEN_DENSITIES)
                       (swap! flags conj :supports-screen-densities))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_SUPPORTS_SMALL_SCREENS)
                       (swap! flags conj :supports-small-screen))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_SUPPORTS_XLARGE_SCREENS)
                       (swap! flags conj :supports-xlarge-screen))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_SYSTEM)
                       (swap! flags conj :system))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_TEST_ONLY)
                       (swap! flags conj :test-only))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_UPDATED_SYSTEM_APP)
                       (swap! flags conj :updated-system-app))
                     (when (bit-and %
                                    ApplicationInfo/FLAG_VM_SAFE_MODE)
                       (swap! flags conj :vm-safe-mode))
                     @flags)
                  (.flags application))
          :install-location (.installLocation application)
          :largest-width-limit-dp (.largestWidthLimitDp application)
          :manage-space-activity-name (.manageSpaceActivityName application)
          :native-library-dir (.nativeLibraryDir application)
          :permission (.permission application)
          :process-name (.processName application)
          :public-source-dir (.publicSourceDir application)
          :requires-smallest-width-dp (.requiresSmallestWidthDp application)
          :resource-dirs (set (.resourceDirs application))
          :seinfo (.seinfo application)
          :shared-library-files (set (.sharedLibraryFiles application))
          :source-dir (.sourceDir application)
          :target-sdk-version (.targetSdkVersion application)
          :task-affinity (.taskAffinity application)
          :theme (load-res-resource-name application
                                     (.theme application))
          :uid (.uid application)
          :ui-options (.uiOptions application)}))

(defn parse-configuration-info
  "parse ConfigurationInfo"
  [^ConfigurationInfo configuration]
  (merge {}
         {:req-gles-version (#(case %
                                ConfigurationInfo/GL_ES_VERSION_UNDEFINED
                                :undefined
                                %)
                             (.reqGlEsVersion configuration))
          :req-input-features (#(let [flags (atom #{})]
                                  (when (bit-and %
                                                 ConfigurationInfo/INPUT_FEATURE_FIVE_WAY_NAV)
                                    (swap! flags conj :five-way-nav))
                                  (when (bit-and %
                                                 ConfigurationInfo/INPUT_FEATURE_HARD_KEYBOARD)
                                    (swap! flags conj :hard-keyboard))
                                  @flags)
                               (.reqInputFeatures configuration))
          :req-keyboard-type (.reqKeyboardType configuration)
          :req-navigation (.reqNavigation configuration)
          :req-touch-screen (.reqTouchScreen configuration)}))

(defn parse-instrumentation-info
  "parse InstrumentationInfo"
  [^InstrumentationInfo instrumentation]
  (merge {}
         (parse-package-item-info instrumentation)
         {:data-dir (.dataDir instrumentation)
          :functional-test (.functionalTest instrumentation)
          :handle-profiling (.handleProfiling instrumentation)
          :native-library-dir (.nativeLibraryDir instrumentation)
          :public-source-dir (.publicSourceDir instrumentation)
          :source-dir (.sourceDir instrumentation)
          :target-package (.targetPackage instrumentation)}))

(defn parse-permission-group-info
  "parse PermissionGroupInfo"
  [^PermissionGroupInfo permission-group]
  (merge {}
         (parse-package-item-info permission-group)
         {:description (load-res-string permission-group
                                    (.descriptionRes permission-group)
                                    (.nonLocalizedDescription permission-group))
          :description-res (.descriptionRes permission-group)
          :flags (#(let [flags (atom #{})]
                     (swap! flags conj
                            (case %
                              PermissionGroupInfo/FLAG_PERSONAL_INFO :personal-info
                              %))
                     @flags)
                  (.flags permission-group))
          :non-localized-description (.nonLocalizedDescription permission-group)
          :priority (.priority permission-group)}))

(defn parse-permission-info
  "parse PermissionInfo"
  [^PermissionInfo permission]
  (merge {}
         (parse-package-item-info permission)
         {:description (load-res-string permission
                                    (.descriptionRes permission)
                                    (.nonLocalizedDescription permission))
          :description-res (.descriptionRes permission)
          :flags (#(let [flags (atom #{})]
                     (when (bit-and %
                                    PermissionInfo/FLAG_COSTS_MONEY)
                       (swap! flags conj :costs-money))
                     @flags)
                  (.flags permission))
          :group (.group permission)
          :non-localized-description (str (.nonLocalizedDescription permission))
          :protection-level (#(let [flags (atom #{})
                                    protection-basic (bit-and %
                                                              PermissionInfo/PROTECTION_MASK_BASE)
                                    protection-flags (bit-and %
                                                              PermissionInfo/PROTECTION_MASK_FLAGS)]
                                (swap! flags conj
                                       (case protection-basic
                                         PermissionInfo/PROTECTION_DANGEROUS
                                         :dangerous
                                         PermissionInfo/PROTECTION_NORMAL
                                         :normal
                                         PermissionInfo/PROTECTION_SIGNATURE
                                         :signature
                                         PermissionInfo/PROTECTION_SIGNATURE_OR_SYSTEM
                                         :signature-or-system
                                         :unknown-protection-basic-level))
                                (when (bit-and protection-flags
                                               PermissionInfo/PROTECTION_FLAG_DEVELOPMENT)
                                  (swap! flags conj :development))
                                (when (bit-and protection-flags
                                               PermissionInfo/PROTECTION_FLAG_SYSTEM)
                                  (swap! flags conj :system))
                                @flags)
                             (.protectionLevel permission))}))

(defn parse-provider-info
  "parse ProviderInfo"
  [^ProviderInfo provider]
  (merge {}
         (parse-component-info provider)
         {:authority (.authority provider)
          :flags (#(let [flags (atom #{})]
                     (when (bit-and %
                                    ProviderInfo/FLAG_SINGLE_USER)
                       (swap! flags conj
                              :single-user))
                     @flags)
                  (.flags provider))
          :grant-uri-permissions (.grantUriPermissions provider)
          :init-order (.initOrder provider)
          :multiprocess (.multiprocess provider)
          :path-permissions (set (.pathPermissions provider))
          :read-permission (.readPermission provider)
          :uri-permission-patterns (set (.uriPermissionPatterns provider))
          :write-permission (.writePermission provider)}))

(defn parse-feature-info
  "parse FeatureInfo"
  [^FeatureInfo feature]
  (merge {}
         {:flags (set (#(let [flags (atom #{})]
                          (when (bit-and %
                                         FeatureInfo/FLAG_REQUIRED)
                            (swap! flags conj :required))
                          @flags)
                       (.flags feature)))
          :name (.name feature)
          :req-gles-version (#(case %
                                FeatureInfo/GL_ES_VERSION_UNDEFINED
                                :undefined
                                %)
                             (.reqGlEsVersion feature))}))

(defn parse-service-info
  "parse ServiceInfo"
  [^ServiceInfo service]
  (merge {}
         (parse-component-info service)
         {:flags (#(let [flags (atom #{})]
                     (when (bit-and %
                                    ServiceInfo/FLAG_ISOLATED_PROCESS)
                       (swap! flags conj :isolated-process))
                     (when (bit-and %
                                    ServiceInfo/FLAG_SINGLE_USER)
                       (swap! flags conj :single-user))
                     (when (bit-and %
                                    ServiceInfo/FLAG_STOP_WITH_TASK)
                       (swap! flags conj :stop-with-task)))
                  (.flags service))
          :permission (.permission service)}))

