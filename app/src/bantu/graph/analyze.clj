(ns bantu.graph.analyze
  (:require [bantu.common :refer [deforder syms->map to-file]]
            [clojure.java.io :as io]
            [selmer.parser :refer [render-file]]
            [clojure.string :as str])
  (:import [java.time LocalDateTime]
           [java.time.format DateTimeFormatter]
           [java.util Locale]))

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

(defn by-build-date [a b]
  (compare (:build-date a) (:build-date b)))

(defn parse-date-from-build-name [file-name]
  (let [[_ time-str] (re-matches #".*build-(.*).txt" file-name)
        formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd-HH-mm-ss" Locale/ENGLISH)]
    (LocalDateTime/parse time-str formatter)))

(defn process-line [line]
  (condp re-matches line
    #"Task '.*:.*:(.*)' finished in (.*)" :>> (fn [[_ task-name duration]] (syms->map task-name duration))
    #" *REBUILD_REASON: (.*)" :>> (fn [[_ reason]] {:rebuild-reason reason})
    nil))

(defn collect-stat [info-file]
  (sort by-task-order (with-open [rdr (io/reader info-file)]
                        (->> (line-seq rdr)
                             (map process-line)
                             (remove nil?)
                             (doall)))))

(defn analyze-build [info-file]
  (let [file-name (. info-file getName)
        build-date (parse-date-from-build-name file-name)
        stats (collect-stat info-file)
        reasons (->> stats (filter :rebuild-reason))
        tasks (->> stats (filter :task-name))]
    (syms->map file-name build-date reasons tasks)))

(def colors ["#111111" "#444444" "#777777" "#AAAAAA" "#DDDDDD"])

(defn parse-duration [dur-str]
  (let [dur-str (str/replace dur-str #"," "")]
    (Float/parseFloat (subs dur-str 0 (- (count dur-str) 2)))))

(defn accumulate-time
  ;; from [{:task-name "..." :duration "5.0 s"}]
  ;; to   [{:task-name "..." :duration 5.0 :acc 2.0}]
  [tasks]
  (loop [cur-tasks tasks acc 0.0 new-tasks []]
    (let [[cur & remains] cur-tasks
          duration (-> cur :duration parse-duration)
          cur-with-acc (assoc cur :duration duration :acc acc)
          acc-task (conj new-tasks cur-with-acc)]
      (if (empty? remains)
        acc-task
        (recur remains (+ acc duration) acc-task)))))

(defn tasks->bar [max-dur height tasks]
  (->> (accumulate-time tasks)
       (map-indexed (fn [idx item] [idx item]))
       (map (fn [[idx {:keys [duration acc]}]]
              {:y-pos (-> acc (/ max-dur) (* height))
               :height (-> duration (/ max-dur) (* height))
               :color (colors (mod idx (count colors)))}))))

(defn render-bars
  ;; bar is vec [{:x-pos 0.0 :y-pos 0.0 :height 0.0 :color "#FFFFFF"}]
  [bar] (->> bar
             (map #(render-file "bantu/graph/bar.html" %))
             (map #(str/replace % #"\n? +" " "))))

(defn render-build-stat [res]
  (let [kotlin-build (-> res io/resource io/file .listFiles)
        stat (->> kotlin-build (map analyze-build) (sort by-build-date))]
    (->> stat (map :tasks) (remove empty?)
         (map (partial tasks->bar 100 64))
         (map-indexed (fn [idx bar]
                        (map (fn [segment] (assoc segment :x-pos (* idx 20.0))) bar)))
         (map render-bars))))

(defn ^{:sidebar "graph" :title "Graph"} graph []
  (render-file "bantu/graph/svg.html" 
               {:bars (->> (render-build-stat "build-reports/kotlin-build-ric") flatten (str/join "\n"))}))

(comment
  (let [date1 (parse-date-from-build-name "icip-build-2023-01-12-14-04-19.txt")
        date2 (parse-date-from-build-name "icip-build-2023-01-12-14-04-20.txt")]
    (compare date2 date1))

  (->> (render-build-stat "build-reports/kotlin-build-ric")
       (to-file "out/bars.edn")))