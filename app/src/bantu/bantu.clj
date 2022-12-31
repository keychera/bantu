(ns bantu.bantu
  (:require [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [funrepo.anki :as anki]
            [org.httpkit.server :refer [as-channel send!]]
            [selmer.parser :refer [render-file]]))

(defn app [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body (render-file "home.html" {})})

(defn connect-anki []
  (let [anki-response (try (anki/connect) (catch Exception _ nil))]
    (if anki-response
      {:status 200
       :body (render-file "connect-success.html" {:anki-connect-ver (:body anki-response)})}
      {:status 200
       :body (render-file "connect-failed.html" {})})))

;; from as-channel source code docs
(defn anki-async-handler [ring-req]
  (as-channel ring-req {:on-open (fn [ch]  (send! ch (connect-anki)))}))

;; ;; https://gist.github.com/borkdude/1627f39d072ea05557a324faf5054cf3
;; (defn router [req]
;;   (let [paths (vec (rest (str/split (:uri req) #"/")))]
;;     (match [(:request-method req) paths]
;;       [:get []] (app req)
;;       [:get ["connect-anki"]] (anki-async-handler req)
;;       [:get ["css" "style.css"]] {:body (slurp (io/resource "public/css/style.css"))}
;;       :else {:status 404 :body "<p>Page not found.</p>"})))


;; /
;; /hello
;; /doc/intro
;; /user/chera

(defn hello [] {:body "hello"})
(defn doc [] {:body "doc"})
(defn intro [] {:body "intro"})
(defn user [id] {:body (str "user is " id)})

(defn router [req]
  (let [paths (-> (:uri req) (str/split #"/") rest vec)
        verb (:request-method req)]
    (match [verb paths]
      [:get []] {:body (render-file "ex/bread.html" {})}
      [verb ["hello"]] (hello)
      [verb ["doc"]] (doc)
      [verb ["doc" "intro"]] (intro)
      [verb ["user" id]] (user id)
      [:get ["css" "style.css"]] {:body (slurp (io/resource "public/css/style.css"))}
      :else {:status 404 :body "not found"})))