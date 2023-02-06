(ns bantu.common
  (:require [babashka.curl :as curl]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [org.httpkit.server :refer [as-channel send!]]))

(defn httpkit-async! [req get-res]
  (as-channel req {:on-open (fn [ch] (send! ch (get-res)))}))

(defn http-get [url opts] (curl/get url opts))

(defmacro syms->map [& syms]
  (zipmap (map keyword syms) syms))

(defmacro deforder [name order-vec]
  `(def ~name (->> (map-indexed (fn [idx itm] [itm idx]) ~order-vec)
                   (into (sorted-map)))))

(defn to-file
  "Write a lazy seq to a file, source: https://gist.github.com/tstout/5ade2d1d1193954013bffd1bf6559411"
  [target seq] 
  (with-open [w (io/writer target)]
    (doseq [line (map #(with-out-str (pprint %)) seq)]
           (.write w line))))
