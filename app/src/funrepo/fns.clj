(ns funrepo.fns)

(defn ^{:bantu true} retrieve
  [^{:type :number} number
   ^{:type :text} text
   ^{:type :checkbox} boolean?
   ^{:type :enum :values ["wizard" "rogue" "healer" "warrior"]} enums
   ^{:type :file} file]
  (Thread/sleep 1000)
  (str "retrieved: " enums " " number " " text  " " boolean? " " file))

(defn ^:bantu do-it [] "やった！")

(defn ^:bantu do-math [inp] (+ 1 (or inp 0)))
