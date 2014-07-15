(ns cnc.plugin.model.figurehead.visualize.helper
  (:use
   [clojure.core.async :as async
    :only [chan 
           close!
           go thread 
           >! <! >!! <!!
           alts! alts!!
           timeout]]
   dorothy.core
   )

  (:require
   [clojure.string :as str]
   [clojure.java.io :as io]
   ))

(defn visualize
  [model viz-root viz-counter]
  (let [g (atom [])
        packages (-> model :packages keys)
        cur (atom 0)]
    (doseq [package packages]
      (let [pkg (get-in model [:packages package])]
        (swap! cur inc)
        ;; subgraphs by packages
        (swap! g conj
               (subgraph package
                         (apply vector 
                                {:label (:name pkg)}
                                (node-attrs {:style :filled
                                             :color (str/join " "
                                                              [(double (* @cur (/ 1 (count packages))))
                                                               1
                                                               1])})
                                (->> (:activities pkg)
                                     (map (fn [[id {:keys [name flags]}]]
                                            (vector
                                             id
                                             {
                                              :label name
                                              :shape 
                                              (cond
                                               (contains? flags
                                                          :category-home)
                                               :house

                                               (contains? flags
                                                          :category-launcher)
                                               :box

                                               :other
                                               :ellipse
                                               )
                                              })))))))))
    ;; edges
    (let [edges (get model :edges)]
      (doseq [from (keys edges)
              to (-> edges (get-in [from :to]) keys)]
        (apply swap! g conj (repeat
                             (get-in edges [from :to to])
                             [from to]))))
    
    ;; draw the whole graph
    (let [d (dot (digraph @g))
          fname-root (str viz-root "_" viz-counter)]
      (spit (str fname-root ".dot") d)
      (save! d (str fname-root ".pdf") {:format :pdf}) 
      (save! d (str fname-root ".png") {:format :png}))))
