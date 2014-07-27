(ns figurehead.api.os.user-manager.user-manager-parser
  "parse User Manager objects into Clojure data structures"
  (:require (figurehead.util [services :as services :refer [get-service]]))
  (import (android.content.pm UserInfo)))

(declare parse-user-info)

(defn parse-user-info
  "parse UserInfo"
  [^UserInfo user-info]
  (merge {}
         {:creation-time (.creationTime user-info)
          :flags (#(let [flags (atom #{})
                         user-type (bit-and %
                                            UserInfo/FLAG_MASK_USER_TYPE)]
                     (when (bit-and user-type
                                    UserInfo/FLAG_ADMIN)
                       (swap! flags conj :admin))
                     (when (bit-and user-type
                                    UserInfo/FLAG_GUEST)
                       (swap! flags conj :guest))
                     (when (bit-and user-type
                                    UserInfo/FLAG_INITIALIZED)
                       (swap! flags conj :initialized))
                     (when (bit-and user-type
                                    UserInfo/FLAG_PRIMARY)
                       (swap! flags conj :primary))
                     (when (bit-and user-type
                                    UserInfo/FLAG_RESTRICTED)
                       (swap! flags conj :restricted))
                     @flags)
                  (.flags user-info))
          :icon-path ^String (.iconPath user-info)
          :id (.id user-info)
          :last-logged-in-time (.lastLoggedInTime user-info)
          :name ^String (.name user-info)
          :partial (.partial user-info)
          :serial-number (.serialNumber user-info)}))
