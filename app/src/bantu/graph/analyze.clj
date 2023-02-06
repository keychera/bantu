(ns bantu.graph.analyze
  (:require [clojure.java.io :as io]
            [selmer.parser :refer [render-file]])
  (:import [java.time LocalDateTime]
           [java.time.format DateTimeFormatter]
           [java.util Locale]))

(defn parse-date-from-build-name [file-name] 
  (let [[_ time-str] (re-matches #".*build-(.*).txt" file-name) 
        formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd-HH-mm-ss" Locale/ENGLISH)]
      (LocalDateTime/parse time-str formatter)))


(defn analyze-build [info-file]
  (let [file-name (. info-file getName)
        build-date (parse-date-from-build-name file-name)]
    {:name file-name 
     :date build-date
     :stats (with-open [rdr (io/reader info-file)]
              (->> (line-seq rdr)
                   (map #(re-matches #"Task '(.*)' finished in (.*)" %))
                   (remove nil?)
                   (doall)))}))

(defn comparator [a b]
  (compare (:date a) (:date b)))

(def build-infos (->> (-> (io/resource "build-reports") io/file .listFiles)
                      first .listFiles))

(->> build-infos
     (map analyze-build) 
     (sort comparator))

(defn ^{:sidebar "graph" :title "Graph"} graph []
  (render-file "bantu/graph/svg.html" {}))

(comment 
  (let [date1 (parse-date-from-build-name "icip-build-2023-01-12-14-04-19.txt")
        date2 (parse-date-from-build-name "icip-build-2023-01-12-14-04-20.txt")]
    (compare date2 date1)))