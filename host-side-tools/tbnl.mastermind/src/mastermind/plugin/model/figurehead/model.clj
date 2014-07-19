(ns mastermind.plugin.model.figurehead.model
  (:require (core [init :as init]
                  [state :as state]
                  [bus :as bus]
                  [plugin :as plugin]))
  (:require
   [clojure.set :as set :refer [union]]
   [clojure.core.async :as async
    :refer [chan 
           close!
           go
           >! <!
           alts!]]
   [clojure.java.io :as io]
   ))

(defn init-model []
  "initialize a new model

{
;; package - activity forms an hierarchy
:packages
{
pkg1
{
:name pkg1
:flags #{:category-home}
:activities {
 :a1 {:name \"a1\" :flags #{:category-home}}
 :a2 {:name \"a2\" :flags #{}} 
 ...}
}
pkg2
{
:name pkg2
:flags #{}
:activities {...}
}

:edges
{
:act1 {:act2 3 :act3 1}
...
}

}"
  (atom {:packages {}
         :edges {}}))

(defn init-state []
  "initialize state space"
  (atom {}))

(defn update-model! [model state news]
  (let [action (:event news)
        content (dissoc news :event)
        instance (:instance content)
        package (:package content)
        package-name (name package)]

    ;; stack is per instance-package
    ;; (when (nil? (get @state instance))
    ;;   (swap! state assoc-in [instance]
    ;;          {:current {
    ;;                     :package nil
    ;;                     :component nil 
    ;;                     }
    ;;           :packages {}}))

    ; ensure the stack is a vector
    (swap! state update-in [instance :packages package]
           vec)

    (let [prev-current (get-in @state [instance :current])
          prev-current-package (:package prev-current)
          prev-current-component (:component prev-current)
          stack (get-in @state [instance :packages package])]

      ;; update state per-instance current package pointer
      (swap! state assoc-in [instance :current :package]
             package)

      (case action
        :starting
        (let [intent-component (:intent-component content)
              prev-intent-component (peek stack)
              intent-action (:intent-action content)
              intent-category (:intent-category content)]

          (swap! state assoc-in [instance :current :component]
                 intent-component)

          (cond  ;; based on category

           (contains? intent-category
                      :android.intent.category.HOME)
           (when (not-empty intent-component)

             ;; update model>packages flags
             (swap! model assoc-in [:packages package :name]
                    package-name)
             (swap! model update-in [:packages package :flags]
                    union
                    #{:category-home})
             (swap! model assoc-in [:packages package :activities intent-component :name]
                    intent-component)
             (swap! model update-in [:packages package :activities intent-component :flags]
                    union
                    #{:category-home})

             false ; no model update
             )


           :other ;; other cases
           (when (not-empty intent-component)

             ;; update per-instance+package state when "new" or "not from launcher"
             (when (or (nil? prev-intent-component)
                       (not (contains? intent-category
                                       :android.intent.category.LAUNCHER)))
               (swap! state update-in [instance :packages package]
                      conj
                      intent-component))

             ;; update model>packages
             (swap! model assoc-in [:packages package :name]
                    package-name)
             (swap! model update-in [:packages package :flags]
                    union
                    (cond
                     :default
                     nil
                     ))
             (swap! model assoc-in [:packages package :activities intent-component :name]
                    intent-component)
             (swap! model update-in [:packages package :activities intent-component :flags]
                    union
                    (cond
                     (contains? intent-category
                                :android.intent.category.LAUNCHER)
                     #{:category-launcher}

                     :default
                     nil
                     ))

             ;; update model>edges
             (when prev-current-component
               (swap! model assoc-in [:edges prev-current-component :name]
                      prev-current-component)
               (swap! model update-in [:edges prev-current-component :to intent-component]
                      #(if (nil? %)
                         1
                         (inc %))))

             true ; model updated
             )))

        :resuming  ; state is changed but no model update
        (when (peek stack)
          (when (= prev-current-package package)
            (swap! state update-in [instance :packages package]
                   pop))
          (swap! state assoc-in [instance :current :component]
                 (peek (get-in @state [instance :packages package])))
          false ; no model update
          )

        :crashed
        (let [
              packages (:packages content)
              ]
          ;; these packages have crashed --> clear their stacks
          (doseq [package packages]
            (swap! state update-in [instance :packages]
                   (dissoc package))))
        
        false ; no model update by default
        ))))

(defn broadcast-model
  "broadcast model"
  [model]
  (bus/say!! :model-update {:type :figurehead
                            :model model}))
