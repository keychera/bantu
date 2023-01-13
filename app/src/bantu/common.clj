(ns bantu.common
  (:require [babashka.curl :as curl] 
            [org.httpkit.server :refer [as-channel send!]]))

(defn httpkit-async! [req get-res]
  (as-channel req {:on-open (fn [ch] (send! ch (get-res)))}))

(defn http-get [url opts] (curl/get url opts))