(ns core.bus
  (:require (core [state :as state]))
  (:require [clojure.core.async
             :as async
             :refer [chan
                     buffer sliding-buffer
                     pub sub unsub
                     mult tap untap
                     >!! <!!]]))

(declare 
         sub-topic unsub-topic get-subscribers
         register-listener unregister-listener get-listeners
         get-message-topic remove-message-topic
         get-topics
         say say!!
         what-is-said what-is-said!!)

(def defaults
  "the defaults"
  (atom
   {
    :bus-main-buffer-size 10000
    :bus-pub-buffer-size 10000
    :get-message-topic (fn [message]
                         (cond
                          (map? message) (:topic message)
                          (sequential? message) (first message)
                          :else message))
    :remove-message-topic (fn [message]
                            (cond
                             (map? message) (:what message)
                             (sequential? message) (rest message)
                             :else nil))
    :pub-buf-fn (fn [topic]
                  (buffer (:bus-pub-buffer-size
                           @defaults)))}))

(let [bus-chan (chan (:bus-main-buffer-size @defaults))
      bus-chan-mult (mult bus-chan)

      bus-chan-1 (chan)
      _1 (tap bus-chan-mult bus-chan-1)
      bus-pub (pub bus-chan-1
                   (:get-message-topic @defaults)
                   (:pub-buf-fn @defaults))

      bus-chan-2 (chan)
      _2 (tap bus-chan-mult bus-chan-2)
      listener-pub (pub bus-chan-2
                        (constantly true)
                        (:pub-buf-fn @defaults))

      topics (atom #{})
      subscribers (atom {})
      listeners (atom #{})]

  (defn sub-topic
    "subscribe the ch(an) to the topic on bus-pub, with close? as in clojure.core.async/sub"
    ([ch topic] (sub-topic ch topic true))
    ([ch topic close?]
       (swap! subscribers update-in [topic]
              (comp set conj) ch)
       (sub bus-pub topic ch close?)))

  (defn unsub-topic
    "unsubscribe the ch(an) from the topic on bus-pub"
    [ch topic]
    (swap! subscribers update-in [topic]
           disj ch)
    (unsub bus-pub topic ch))

  (defn get-subscribers
    "get subscribers"
    []
    @subscribers)

  (defn register-listener
    "register as a listener of all messages over bus-chan"
    ([ch] (register-listener ch true))
    ([ch close?]
       (swap! listeners
              (comp set conj) ch)
       (sub listener-pub true ch close?)))

  (defn unregister-listener
    "undo register-listener"
    [ch]
    (swap! listeners
           disj ch)
    (unsub listener-pub true ch))

  (defn get-listeners
    "get listeners"
    []
    @listeners)

  (defn get-topics
    "get all said topics"
    []
    @topics)

  (defn get-message-topic
    "get the topic of the message"
    [message]
    ((:get-message-topic @defaults) message))

  (defn remove-message-topic
    "remove the topic from the message"
    [message]
    ((:remove-message-topic @defaults) message))



  (defn say
    "say what with the topic"
    ([topic what chan-op] (say topic what false chan-op))
    ([topic what verbose? chan-op]
       (swap! topics conj topic)
       (let [content {:topic topic :what what}]
         (chan-op bus-chan content)
         (when verbose?
           (prn {:content content
                 :topics (get-topics)
                 :subscribers (get-subscribers)
                 :listeners (get-listeners)})))))

  

  ;; say! is omitted because >! may not work with Clojure on Android for SDK 18
  ;; !!! use say! within go block may deadlock
  ;; (defn say!
  ;;   "say what with the topic (>! as chan-op)"
  ;;   ([topic what] (say! topic what false))    
  ;;   ([topic what verbose?]
  ;;      (let [chan-op (fn [ch val]
  ;;                      (go >! ch val))]
  ;;        (say topic what verbose? chan-op))))

  (defn say!!
    "say what with the topic (>!! as chan-op)"
    ([topic what] (say!! topic what false))
    ([topic what verbose?]
       (let [chan-op (fn [ch val]
                       (>!! ch val))]
         (say topic what verbose? chan-op))))

  (defn what-is-said
    "get what is from the sub-ch"
    ([sub-ch chan-op] (what-is-said sub-ch false chan-op))
    ([sub-ch verbose? chan-op]
       (let [said (chan-op sub-ch)]
         (when verbose?
           (prn [:said said]))
         (cond (map? said)
               (:what said)

               :else
               nil))))

  (defn what-is-said!!
    "get what is said on sub-ch (<!! as chan-go)"
    ([sub-ch] (what-is-said!! sub-ch false))
    ([sub-ch verbose?]
       (let [chan-op (fn [ch]
                       (<!! ch))]
         (what-is-said sub-ch verbose? chan-op)))))
