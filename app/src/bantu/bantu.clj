(ns bantu.bantu
  (:require [clojure.core.match :as match]
            [clojure.string :as str] 
            [selmer.parser :refer [render-file]]
            [clojure.java.io :as io]))

(defn app [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body (render-file "home.html" {:button-text "[Selmer] click me!"})})

(defn connect-anki []
  (Thread/sleep 1000)
  {:status 200
   :body (render-file "connect-failed.html" {})})

;; https://gist.github.com/borkdude/1627f39d072ea05557a324faf5054cf3
(defn router [req]
  (let [paths (vec (rest (str/split (:uri req) #"/")))]
    (match/match [(:request-method req) paths]
      [:get []] (app req)
      [:get ["connect-anki"]] (connect-anki)
      [:get ["css" "style.css"]] {:body (slurp (io/resource "public/css/style.css"))}
      :else {:status 404 :body "<p>Page not found.</p>"})))
