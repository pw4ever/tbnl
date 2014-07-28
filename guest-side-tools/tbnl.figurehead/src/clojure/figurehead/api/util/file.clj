(ns figurehead.api.util.file
  "file utils"
  (:require (core [state :as state :refer [defcommand]]))  
  (:require (figurehead.util [services :as services :refer [get-service]]))
  (:require [clojure.string :as str]
            [clojure.java.io :as io])  
  (:import (android.content.res AssetManager
                                Resources)
           (android.util Base64)
           (java.io File)
           (org.apache.commons.io FileUtils)))

(declare decode-content-to-bytes encode-content-from-bytes
         decode-content-to-string encode-content-from-string
         decode-bytes-to-string encode-bytes-from-string
         
         write-file read-file)

(defcommand decode-content-to-bytes
  "decode from Base64-encoded content to bytes"
  [{:keys [^String content]
    :as args}]
  {:pre [content]}
  (when (and content)
    ^bytes (Base64/decode content
                          Base64/DEFAULT)))

(defcommand encode-content-from-bytes
  "encode to Base64-encoded content from bytes"
  [{:keys [^bytes bytes]
    :as args}]
  {:pre [bytes]}
  (when (and bytes)
    (Base64/encodeToString bytes
                           (bit-or Base64/NO_WRAP
                                   0))))

(defcommand decode-content-to-string
  "decode from Base64-encoded content to string"
  [{:keys [^String content]
    :as args}]
  {:pre [content]}
  (when (and content)
    (decode-bytes-to-string {:bytes (decode-content-to-bytes {:content content})})))

(defcommand encode-content-from-string
  "encode to Base64-encoded content from string"
  [{:keys [^String string]
    :as args}]
  {:pre [string]}
  (when (and string)
    (encode-content-from-bytes {:bytes (encode-bytes-from-string {:string string})})))

(defcommand decode-bytes-to-string
  "decode from bytes to UTF-8 string"
  [{:keys [^bytes bytes]
    :as args}]
  {:pre [bytes]}
  (when (and bytes)
    (String. bytes "UTF-8")))

(defcommand encode-bytes-from-string
  "encode to bytes from UTF-8 string"
  [{:keys [^String string]
    :as args}]
  {:pre [string]}
  (when (and string)
    (.getBytes string "UTF-8")))

(defcommand write-file
  "write to file from string, Base64-encoded content, or bytes"
  [{:keys [file
           ^String string
           ^String content
           ^bytes bytes]
    :as args}]
  {:pre [file (or content string bytes)]}
  (when (and file (or content string bytes))
    (with-open [the-file (io/output-stream (io/file file))]
      (let [bytes (cond string
                        (encode-bytes-from-string string)

                        content
                        (decode-content-to-bytes content)

                        bytes
                        bytes)]
        (.write the-file ^bytes bytes)))))


(defcommand read-file
  "read from file to string, Base64-encoded content, or bytes"
  [{:keys [file
           string?
           content?
           bytes?]
    :as args}]
  {:pre [file (or string? content? bytes?)]}
  (when (and file (or string? content? bytes?))
    (let [^bytes bytes (FileUtils/readFileToByteArray (io/file file))]
      (cond string?
            (decode-bytes-to-string {:bytes bytes})

            content?
            (encode-content-from-bytes {:bytes bytes})

            bytes?
            bytes))))
