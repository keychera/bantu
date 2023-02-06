(ns bantu.graph.analyze
  (:require [bantu.common :refer [deforder syms->map]]
            [clojure.java.io :as io]
            [selmer.parser :refer [render-file]])
  (:import [java.time LocalDateTime]
           [java.time.format DateTimeFormatter]
           [java.util Locale]))

(defn parse-date-from-build-name [file-name]
  (let [[_ time-str] (re-matches #".*build-(.*).txt" file-name)
        formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd-HH-mm-ss" Locale/ENGLISH)]
    (LocalDateTime/parse time-str formatter)))

(defn process-line [line]
  (condp re-matches line
    #"Task '.*:.*:(.*)' finished in (.*)" :>> (fn [[_ task-name duration]] (syms->map task-name duration))
    #" *REBUILD_REASON: (.*)" :>> (fn [[_ reason]] {:rebuild-reason reason})
    nil))

(deforder task-order [:compileKotlin
                      :compileJava
                      :processResources
                      :classes
                      :jar
                      :inspectClassesForKotlinIC
                      :compileTestKotlin
                      :compileTestJava
                      :processTestResources
                      :testClasses
                      :icip
                      :test])

(defn by-task-order [{t1 :task-name} {t2 :task-name}]
  (compare (task-order (keyword t1)) (task-order (keyword t2))))

(defn collect-stat [info-file]
  (sort by-task-order (with-open [rdr (io/reader info-file)]
                        (->> (line-seq rdr)
                             (map process-line)
                             (remove nil?)
                             (doall)))))

(defn analyze-build [info-file]
  (let [file-name (. info-file getName)
        build-date (parse-date-from-build-name file-name)]
    {:name file-name
     :date build-date
     :stats (collect-stat info-file)}))

(defn by-build-date [a b]
  (compare (:date a) (:date b)))

(def build-infos (->> (-> (io/resource "build-reports") io/file .listFiles)
                      first .listFiles))

(->> (-> "build-reports/kotlin-build-ric" io/resource io/file .listFiles)
     (map analyze-build)
     (sort by-build-date))

(defn ^{:sidebar "graph" :title "Graph"} graph []
  (render-file "bantu/graph/svg.html" {}))

(comment
  (let [date1 (parse-date-from-build-name "icip-build-2023-01-12-14-04-19.txt")
        date2 (parse-date-from-build-name "icip-build-2023-01-12-14-04-20.txt")]
    (compare date2 date1)))