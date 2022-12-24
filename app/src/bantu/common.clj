(ns bantu.common
  (:require [clojure.string :as str]
            [hiccup2.core :refer [html]]))

(def f-sep (java.io.File/separator))
(defn from-here
  ([]     (from-here ""))
  ([filename]  (str (as-> *file* it (str/split it (re-pattern f-sep)) (drop-last it) (str/join f-sep it)) f-sep filename)))

(defn html-str
  ([input] (str (html input)))
  ([input & rest] (str (html input) (->> rest (map #(html %)) (apply str)))))