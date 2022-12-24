(ns bantu.bantu
  (:require [bantu.common :refer [html-str]]
            [cheshire.core :as json]
            [clojure.core.match :as match]
            [clojure.string :as str] 
            [org.httpkit.server :refer [as-channel send!]]
            [selmer.parser :refer [render-file]]
            [clojure.java.io :as io]))

(defn app [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body (render-file "home.html" {:button-text "[Selmer] click me!"})})

(defn clicked [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body (render-file "clicked.html" {:content "BOO from Selmer/HTML"})})

;; from httpkit as-channel sourcecode
(defn websocket [req]
  (if (:websocket? req)
    (as-channel req
                {:on-open    (fn [ch]         (println "[bantu] on-open"))
                 :on-close   (fn [ch status]  (println "[bantu] on-close"   status))
                 :on-receive (fn [ch message]
                               (println "on-receive:" message)
                               (send! ch {:body (html-str [:form {:id "form" :ws-send "true"}
                                                           [:input {:name "chat_message"}]]
                                                          [:div {:id "chats" :hx-swap-oob "beforeend"}
                                                           [:div {} (let [json-map (json/parse-string message)]
                                                                      (get json-map "chat_message" "hmm??"))]])}))})
    {:status 404 :body "<h1>Hmm?</h1>"}))

;; https://gist.github.com/borkdude/1627f39d072ea05557a324faf5054cf3
(defn router [req]
  (let [paths (vec (rest (str/split (:uri req) #"/")))]
    (match/match [(:request-method req) paths]
      [:get []] (app req)
      [:get ["ws"]] (websocket req)
      [:post ["clicked"]] (clicked req)
      [:get ["css" "style.css"]] {:body (slurp (io/resource "public/css/style.css"))}
      :else {:status 404 :body "<p>Page not found.</p>"})))
