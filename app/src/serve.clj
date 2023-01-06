(ns serve
  (:require [bantu.bantu :as bantu]
            [bantu.common :refer [httpkit-async!]]
            [org.httpkit.server :refer [run-server]]))

(defonce port 4242)
(defonce url (str "http://localhost:" port "/"))

(defn async-wrapper [server req]
  (let [res (server req)]
    (if-not (:as-async res) res
            (httpkit-async! req (:as-async res)))))

(defn -main [& _]
  (let [router (partial async-wrapper #'bantu/router)]
    (run-server router {:port port :thread 12})
    (println "[panas] serving" url)
    @(promise)))
