(ns bantu.common
  (:require [clojure.string :as str]))

(def f-sep (java.io.File/separator))
(defn from-here
  ([]     (from-here ""))
  ([filename]  (str (as-> *file* it (str/split it (re-pattern f-sep)) (drop-last it) (str/join f-sep it)) f-sep filename)))
