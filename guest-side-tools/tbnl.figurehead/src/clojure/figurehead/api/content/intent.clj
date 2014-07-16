(ns figurehead.api.content.intent
  (:import (android.content Intent
                            ComponentName)
           (android.os Bundle)
           (android.net Uri)))

(declare make-intent)

(defn make-intent
  "make an Intent object

reference: https://github.com/android/platform_frameworks_base/blob/android-4.3_r3.1/cmds/am/src/com/android/commands/am/Am.java#L479"
  [& args]
  (let [args (into {} (map vec (partition 2 args)))
        intent (Intent.)]

    ;; action
    (when-let [action (:action args)]
      (.setAction ^Intent intent action))

    ;; a seq of categories
    (when-let [categories (:categories args)]
      (doseq [category categories]
        (.addCategory ^Intent intent category)))

    ;; a seq of extras
    (when-let [extras (:extras args)]
      (doseq [[key val] extras]
        (.putExtra ^Intent intent key val)))

    ;; package
    (when-let [package (:package args)]
      (.setPackage ^Intent intent package))

    ;; component
    (when-let [component (:component args)]
      (.setComponent ^Intent intent
                     (ComponentName/unflattenFromString component)))

    ;; flags can be either a number or a seq of individual flags (Intent/FLAG_*)
    (when-let [flags (:flags args)]
      (cond (number? flags)
            (.setFlags ^Intent intent
                       flags)

            (seq? flags)
            (doseq [flag flags]
              (.addFlags ^Intent intent flag))))

    ;; data and type
    (let [data ^Uri (:data args)
          type ^String (:type args)]
      (.setDataAndType ^Intent intent data type))

    ;; free-form wild-card
    (when-let [wild-card (:wild-card args)]
      (let [wild-card (str wild-card)]
        (cond (>= (.indexOf wild-card ":") 0)
              (do
                ;; wild-card is a URI; fully parse it
                (.setData intent (Intent/parseUri wild-card Intent/URI_INTENT_SCHEME)))

              (>= (.indexOf wild-card "/") 0)
              (do
                ;; wild-card is a component name; build an intent to launch it
                (.setAction ^Intent intent Intent/ACTION_MAIN)
                (.addCategory ^Intent intent Intent/CATEGORY_LAUNCHER)
                (.setComponent ^Intent intent (ComponentName/unflattenFromString wild-card)))

              :else
              (do
                ;; assume wild-card is a package name
                (.setAction ^Intent intent Intent/ACTION_MAIN)
                (.addCategory ^Intent intent Intent/CATEGORY_LAUNCHER)
                (.setPackage ^Intent intent wild-card)))))
    
    intent))
