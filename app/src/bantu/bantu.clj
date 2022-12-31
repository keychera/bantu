(ns bantu.bantu
  (:require [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [funrepo.anki :as anki]
            [org.httpkit.server :refer [as-channel send!]]
            [selmer.parser :refer [render-file]]))

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

;; components
(defn app
  ([] (app nil))
  ([main] (render-file "ex/bread.html" {:render-main main})))

(defn hello [] "hello")
(defn doc [] (render-file "ex/doc.html" {}))
(defn intro [] "intro")
(defn user [id] (str "user is " id))

(defn partial? [req] (= "p" (:query-string req)))

(defn component-route [partial sub]
  (if partial {:body sub} {:body (app sub)}))

(defn router [req]
  (let [paths (-> (:uri req) (str/split #"/") rest vec)
        verb (:request-method req)
        route (partial component-route (partial? req))]
    (match [verb paths]
      [:get []] {:body (app)}
      [:get ["hello"]] (route (hello))
      [:get ["doc"]] (route (doc))
      [:get ["doc" "intro"]] (route (intro))
      [:get ["user" id]] (route (user id))
      [:get ["css" "style.css"]] {:body (slurp (io/resource "public/css/style.css"))}
      :else {:status 404 :body "not found"})))