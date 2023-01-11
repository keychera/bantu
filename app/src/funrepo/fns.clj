(ns funrepo.fns)

(defn ^{:bantu true} retrieve
  [^{:type :number} number
   ^{:type :text} text
   ^{:type :checkbox} boolean?
   ^{:type :enum :values [:wizard :rogue :healer :warrior]} enums
   ^{:type :coll} coll
   ^{:type :multi} multiline
   ^{:type :file} file]
  (str enums " " number " " text  " " boolean? " " coll " " multiline " " file))

(defn ^:bantu do-it [] "やった！")

(defn do-math [] (+ 1 2))
