(ns bantu.common
  (:require [babashka.curl :as curl]
            [clojure.string :as str]
            [org.httpkit.server :refer [as-channel send!]]
            [pod.retrogradeorbit.bootleg.markdown :refer [markdown]]
            [pod.retrogradeorbit.bootleg.utils :refer [convert-to]]))

(defn httpkit-async! [req get-res]
  (as-channel req {:on-open (fn [ch] (send! ch (get-res)))}))

(defn http-get [url opts] (curl/get url opts))