(ns funrepo.fns)

(def job [:wizard :rogue :healer :warrior])

(defn ^:bantu retrieve
  [^:int integer
   ^:float float
   ^:string text
   ^:boolean boolean?
   ^{:enum job} enums
   ^:coll coll
   ^:multi multiline
   ^:file file]
  (str enums " " integer " " float " " text  " " boolean? " " coll " " multiline " " file))

(defn ^:bantu do-it [] "やった！")

(defn do-math [] (+ 1 2))
