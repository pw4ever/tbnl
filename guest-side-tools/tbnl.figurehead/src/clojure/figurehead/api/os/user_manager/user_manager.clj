(ns figurehead.api.os.user-manager.user-manager
  "manager users"
  (:require (core [state :as state :refer [defcommand]]))  
  (:require (figurehead.util [services :as services :refer [get-service]]))
  (:require [figurehead.api.os.user-manager.user-manager-parser :as parser])
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.set :as set :refer [subset?]])  
  (:import (android.content.pm UserInfo)
           (android.os IUserManager
                       UserHandle
                       UserManager)))

(declare create-user remove-user wipe-user set-user-name
         find-users find-user get-user-handle
         list-users get-max-users
         set-guest-enabled enable-guest disable-guest)

(defcommand create-user
  "create a user"
  [{:keys [name]
    :as args}]
  (when name
    (let [^IUserManager user-manager (get-service :user-manager)]
      (let [name (str name)
            ^UserInfo info (.createUser user-manager name 0)]
        (when info
          (parser/parse-user-info info))))))

(defcommand remove-user
  "remove a user"
  [{:keys [name
           serial-number
           id
           handle]
    :as args}]
  (when (or name serial-number id handle)
    (let [^IUserManager user-manager (get-service :user-manager)
          handle (get-user-handle args)]
      (when handle
        (.removeUser user-manager handle)))))

(defcommand wipe-user
  "wipe a user"
  [{:keys [name
           serial-number
           id
           handle]
    :as args}]
  (when (or name serial-number id handle)
    (let [^IUserManager user-manager (get-service :user-manager)
          handle (get-user-handle args)]
      (when handle
        (.wipeUser user-manager handle)))))

(defcommand set-user-name
  "set user to a new name"
  [{:keys [name
           serial-number
           id
           handle

           new-name]
    :as args}]
  (when (and new-name
             (or name serial-number id handle))
    (let [^IUserManager user-manager (get-service :user-manager)
          handle (get-user-handle args)]
      (when handle
        (.setUserName user-manager handle new-name)))))

(defcommand find-users
  "find users by name, serial-number, id, or flags"
  [{:keys [name
           serial-number
           id
           flags]}]
  (when (or name serial-number id flags)
    (let [users (list-users {})]
      (cond name
            (filter #(= name (:name %))
                    users)

            serial-number
            (filter #(= serial-number (:serial-number %))
                    users)

            id
            (filter #(= id (:id %))
                    users)

            flags
            (filter #(subset? flags (:flags %))
                    users)))))

(defcommand find-user
  "find a user by name, serial-number, or id"
  [{:keys [name
           serial-number
           id]
    :as args}]
  (when (or name serial-number id)
    (first (find-users args))))

(defcommand get-user-handle
  "get user handle"
  [{:keys [name
           serial-number
           id
           handle]
    :as args}]
  (cond handle
        handle

        (or name serial-number id)
        (let  [^IUserManager user-manager (get-service :user-manager)
               user (find-user (merge {}
                                      (when name
                                        {:name name})
                                      (when serial-number
                                        {:serial-number serial-number})
                                      (when id
                                        {:id id})))]
          (when user
            (.getUserHandle user-manager (:serial-number user))))))

(defcommand list-users
  "list all users"
  [{:keys []
    :as args}]
    (let [^IUserManager user-manager (get-service :user-manager)]
      (into #{}
            (map parser/parse-user-info
                 (.getUsers user-manager false)))))

(defcommand get-max-users
  "get max number of supported users"
  [{:keys []
    :as args}]
  (UserManager/getMaxSupportedUsers))


(defcommand set-guest-enabled
  "set guest enable flag"
  [{:keys [enabled?]
    :as args}]
  (let [^IUserManager user-manager (get-service :user-manager)]
    (.setGuestEnabled user-manager enabled?)))

(defcommand enable-guest
  "enable guest"
  [{:keys []
    :as args}]
  (set-guest-enabled {:enabled? true}))

(defcommand disable-guest
  "disable guest"
  [{:keys []
    :as args}]
  (set-guest-enabled {:enabled? false}))
