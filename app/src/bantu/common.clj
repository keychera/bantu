(ns bantu.common
  (:require [babashka.curl :as curl] 
            [org.httpkit.server :refer [as-channel send!]]))

(defn httpkit-async! [req get-res]
  (as-channel req {:on-open (fn [ch] (send! ch (get-res)))}))

(defn http-get [url opts] (curl/get url opts))

(defmacro syms->map [& syms]
  (zipmap (map keyword syms) syms))

(defmacro deforder [name order-vec]
  `(def ~name (->> (map-indexed (fn [idx itm] [itm idx]) ~order-vec)
                   (into (sorted-map)))))
