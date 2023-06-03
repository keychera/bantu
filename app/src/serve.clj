(ns serve
  (:require [babashka.cli :as cli]
            [babashka.process :as process]
            [bantu.bantu :as bantu]
            [clojure.pprint :refer [pprint]]
            [org.httpkit.server :refer [run-server]]))

(defonce port 4242)
(defonce url (str "http://localhost:" port "/"))


(defn -main [& args]
  (try (let [{:keys [chrome]} (:opts (cli/parse-args args {:require [:chrome]}))
             router bantu/router]
         (run-server router {:port port :thread 12})
         (println "[panas] serving" url)
         (process/shell chrome (str "--app=" url) (str "--user-data-dir=" (System/getProperty "java.io.tmpdir") "bantu")))
       (catch Throwable e (pprint (:via (Throwable->map e))))))
