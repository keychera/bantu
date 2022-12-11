(ns bantu.watcher
  (:require [babashka.pods :as pods]
            [bantu.bantu :refer [start-bantuin stop-bantuin url]]
            [bantu.common :refer [from-here]]))

;; inspiration https://github.com/babashka/book/blob/master/script/watch.clj
(pods/load-pod 'org.babashka/filewatcher "0.0.1")
(require '[pod.babashka.filewatcher :as fw])

(def here (from-here))

(defn -main [& _]
  (println "[bantu] starting")
  (start-bantuin)
  (println "[bantu] serving" url)
  (fw/watch here
            (fn [event]
              (when (= :write (:type event))
                (println "=== ===")
                (println "[bantu] stopping" url)
                (stop-bantuin)
                (try
                  (println "[bantu] starting")
                  (load-file (from-here "common.clj"))
                  (load-file (from-here "bantu.clj"))
                  (start-bantuin)
                  (println "[bantu] serving" url)
                  (catch Exception e
                    (println "[bantu][error]" (.getMessage e)))))))
  @(promise))