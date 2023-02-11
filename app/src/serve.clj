(ns serve
  (:require [bantu.bantu :as bantu] 
            [org.httpkit.server :refer [run-server]]))

(defonce port 4242)
(defonce url (str "http://localhost:" port "/"))


(defn -main [& _]
  (let [router bantu/router]
    (run-server router {:port port :thread 12})
    (println "[panas] serving" url)
    @(promise)))
