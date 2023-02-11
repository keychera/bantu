(ns user
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def app-dir (some-> (io/resource "pivot") .getPath (str/split (re-pattern (str "\\Q" fs/file-separator "\\E"))) drop-last drop-last
                    ;;  (some->> (reduce #(str %1 "/" %2)))
                    ;;  (str "/app")
                     ))

(some-> (io/resource "pivot") .toURI (.resolve "../app") .toString (subs 6))
