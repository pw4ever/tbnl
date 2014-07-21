;;; https://github.com/android/platform_frameworks_base/blob/android-4.3_r3.1/cmds/am/src/com/android/commands/am/Am.java#L479
(ns figurehead.api.content.intent
  (:import (android.content Intent
                            ComponentName)
           (android.os Bundle)
           (android.net Uri)))

(declare make-intent)

(defn make-intent
  "make an Intent object"
  [{:keys [action
           categories
           extras
           package
           component
           flags
           ^Uri data
           type
           wild-card
           ]
    :as args}]
  (let [intent (Intent.)]

    ;; action
    (when action
      (.setAction ^Intent intent action))

    ;; a seq of categories
    (when categories
      (doseq [category categories]
        (.addCategory ^Intent intent category)))

    ;; a seq of extras
    (when extras
      (doseq [[key val] extras]
        (.putExtra ^Intent intent key val)))

    ;; package
    (when package
      (.setPackage ^Intent intent package))

    ;; component
    (when component
      (.setComponent ^Intent intent
                     (ComponentName/unflattenFromString component)))

    ;; flags can be either a number or a seq of individual flags (Intent/FLAG_*)
    (when flags
      (cond (number? flags)
            (.setFlags ^Intent intent
                       flags)

            (seq? flags)
            (doseq [flag flags]
              (.addFlags ^Intent intent flag))))

    ;; data and type
    (.setDataAndType ^Intent intent data type)

    ;; free-form wild-card
    (when wild-card
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
