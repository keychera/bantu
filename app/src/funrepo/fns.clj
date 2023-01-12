(ns funrepo.fns)

(defn ^{:bantu true} retrieve
  [^{:type :number} number
   ^{:type :text} text
   ^{:type :checkbox} boolean?
   ^{:type :enum :values [:wizard :rogue :healer :warrior]} enums
   ^{:type :coll} coll
   ^{:type :multi} multiline
   ^{:type :file} file]
  (str "retrieved: " enums " " number " " text  " " boolean? " " coll " " multiline " " file))

(defn ^:bantu do-it [] "やった！")

(defn ^:bantu do-math [inp] (+ 1 (or inp 0)))
