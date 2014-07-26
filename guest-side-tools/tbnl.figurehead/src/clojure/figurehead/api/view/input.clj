;;; https://github.com/android/platform_frameworks_base/blob/android-4.3_r3.1/cmds/input/src/com/android/commands/input/Input.java
(ns figurehead.api.view.input
  "input (Input Events) wrapper"
  (:require (core [state :as state :refer [defcommand]]))  
  (:require (figurehead.util [services :as services :refer [get-service]]))
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import (android.hardware.input InputManager)
           (android.os SystemClock)
           (android.view InputDevice
                         KeyCharacterMap
                         KeyEvent
                         MotionEvent)))

(declare
 ;; porcelain
 text key-event tap swipe
 touchscreen touchpad touch-navigation
 trackball
 ;; plumbing
 send-text send-key-event
 send-tap send-swipe send-move
 inject-key-event inject-motion-event)

;;; porcelain

(defcommand text
  [{:keys [^String text]
    :as args}]
  (when text
    (send-text (merge args
                      {:text text}))))

(defcommand key-event
  [{:keys [^String key-code meta-state]
    :or {meta-state 0}
    :as args}]
  (let [str-to-key-code (fn [str-key-code prefix]
                          (let [key-code (KeyEvent/keyCodeFromString str-key-code)]
                            (if (= key-code KeyEvent/KEYCODE_UNKNOWN)
                              (KeyEvent/keyCodeFromString (str prefix
                                                               (str/upper-case str-key-code)))
                              key-code)))]
    (let [key-code (cond (number? key-code)
                         key-code

                         (string? key-code)
                         (str-to-key-code key-code "KEYCODE_"))
          meta-state (cond (number? meta-state)
                           meta-state
                           
                           (string? meta-state)
                           (str-to-key-code meta-state "META_")
                           
                           (sequential? meta-state)
                           (reduce bit-or
                                   0
                                   (map #(str-to-key-code % "META_") meta-state)))]
      (when (and key-code meta-state)
        (send-key-event (merge args
                               {:key-code key-code
                                :meta-state meta-state}))))))

(defcommand tap
  [{:keys [x y]
    :as args}]
  (when (and x y)
    (send-tap (merge args
                     {:input-source InputDevice/SOURCE_TOUCHSCREEN
                      :x x
                      :y y}))))

(defcommand swipe
  [{:keys [x1 y1 x2 y2]
    :as args}]
  (when (and x1 y1 x2 y2)
    (send-swipe (merge args
                       {:input-source InputDevice/SOURCE_TOUCHSCREEN
                        :x1 x1 :y1 y1
                        :x2 x2 :y2 y2
                        :duration -1}))))

(let [common-procedure (fn [input-source action args]
                         (case action
                           :tap
                           (do
                             (let [{:keys [x y]} args]
                               (when (and x y)
                                 (send-tap (merge args
                                                  {:input-source input-source
                                                   :x x :y y})))))

                           :swipe
                           (do
                             (let [{:keys [x1 y1 x2 y2 duration]} args]
                               (when (and x1 y1 x2 y2)
                                 (send-swipe (merge args
                                                    {:input-source input-source
                                                     :x1 x1 :y1 y1
                                                     :x2 x2 :y2 y2
                                                     :duration (if duration
                                                                 duration
                                                                 -1)})))))

                           :else))]
  (defcommand touchscreen
    [{:keys [action]
      :as args}]
    (common-procedure InputDevice/SOURCE_TOUCHSCREEN (or action :tap) args))

  (defcommand touchpad
    [{:keys [action]
      :as args}]
    (common-procedure InputDevice/SOURCE_TOUCHPAD (or action :tap) args))

  (defcommand touch-navigation
    [{:keys [action]
      :as args}]
    (common-procedure InputDevice/SOURCE_TOUCH_NAVIGATION (or action :tap) args)))

(defcommand trackball
  [{:keys [action]
    :as args}]
  (let [input-source InputDevice/SOURCE_TRACKBALL]
    (case action
      :press
      (do
        (send-tap (merge args
                         {:input-source input-source
                          :x 0.0 :y 0.0})))

      :roll
      (do
        (let [{:keys [dx dy]} args]
          (when (and dx dy)
            (send-move (merge args
                              {:input-source input-source
                               :dx dx :dy dy})))))

      :else)))

;;; plumbing

(defcommand send-text
  "convert the characters of string text into key events and send to the device"
  [{:keys [^String text]
    :as args}]
  (let [kcm ^KeyCharacterMap (KeyCharacterMap/load KeyCharacterMap/VIRTUAL_KEYBOARD)]
    (doseq [^KeyEvent event (.getEvents kcm (.toCharArray text))]
      (inject-key-event {:event event}))))

(defcommand send-key-event
  "send key event"
  [{:keys [key-code
           meta-state]
    :or {meta-state 0}
    :as args}]
  (let [now (SystemClock/uptimeMillis)
]
    (inject-key-event {:event (KeyEvent. now now KeyEvent/ACTION_DOWN key-code 0 meta-state
                                         KeyCharacterMap/VIRTUAL_KEYBOARD 0 0 InputDevice/SOURCE_KEYBOARD)})
    (inject-key-event {:event (KeyEvent. now now KeyEvent/ACTION_UP key-code 0 meta-state
                                         KeyCharacterMap/VIRTUAL_KEYBOARD 0 0 InputDevice/SOURCE_KEYBOARD)})))

(defcommand send-tap
  "send tap event"
  [{:keys [input-source
           x y]
    :or {input-source InputDevice/SOURCE_TOUCHSCREEN}
    :as args}]
  (let [now (SystemClock/uptimeMillis)]
    (inject-motion-event {:input-source input-source
                          :action MotionEvent/ACTION_DOWN
                          :when now
                          :x x
                          :y y
                          :pressure 1.0})
    (inject-motion-event {:input-source input-source
                          :action MotionEvent/ACTION_UP
                          :when now
                          :x x
                          :y y
                          :pressure 0.0})))

(defcommand send-swipe
  "send swipe event"
  [{:keys [input-source
           x1 y1
           x2 y2
           duration]
    :or {input-source InputDevice/SOURCE_TOUCHSCREEN}
    :as args}]
  (let [now (SystemClock/uptimeMillis)
        duration (if (>= duration 0) duration 300)
        start-time now
        end-time (+ now duration)
        lerp (fn [x y alpha]
               (+ x
                  (* alpha
                     (- y x))))]
    (inject-motion-event {:input-source input-source
                          :action MotionEvent/ACTION_DOWN
                          :when now
                          :x x1
                          :y y1
                          :pressure 1.0})
    (loop [now now]
      (if (< now end-time)
        (do
          (let [elapsed-time (- now start-time)
                alpha (/ (float elapsed-time) duration)]
            (inject-motion-event {:input-source input-source
                                  :action MotionEvent/ACTION_MOVE
                                  :when now
                                  :x (lerp x1 x2 alpha)
                                  :y (lerp y1 y2 alpha)
                                  :pressure 1.0}))
          (recur (SystemClock/uptimeMillis)))
        (inject-motion-event {:input-source input-source
                              :action MotionEvent/ACTION_UP
                              :when now
                              :x x2
                              :y y2
                              :pressure 0.0})))))

(defcommand send-move
  "send a zero-pressure move event"
  [{:keys [input-source
           dx dy]
    :or {input-source InputDevice/SOURCE_TOUCHSCREEN}
    :as args}]
  (let [now (SystemClock/uptimeMillis)]
    (inject-motion-event {:input-source input-source
                          :action MotionEvent/ACTION_MOVE
                          :when now
                          :x dx
                          :y dy
                          :pressure 0.0})))

(defcommand inject-key-event
  "inject a key event"
  [{:keys [^KeyEvent event]
    :as args}]
  (.. (InputManager/getInstance)
      (injectInputEvent event
                        InputManager/INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH)))

(defcommand inject-motion-event
  "inject a motion event"
  [{:keys [input-source
           action
           when
           meta-state
           x y
           pressure]
    :or {input-source InputDevice/SOURCE_TOUCHSCREEN
         meta-state 0}    
    :as args}]
  (let [size 1.0
        precision-x 1.0
        precision-y 1.0
        device-id 0
        edge-flags 0

        event (MotionEvent/obtain when when action x y pressure
                                  size meta-state precision-x precision-y device-id edge-flags)]
    (.setSource event input-source)
    (.. (InputManager/getInstance)
        (injectInputEvent event
                          InputManager/INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH))))
