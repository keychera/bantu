(ns bantu.bantu
  (:require [clojure.core.match :as match]
            [clojure.string :as str]
            [hiccup2.core :refer [html]]
            [cheshire.core :as json]
            [org.httpkit.server :refer [as-channel run-server send!]]))

(def port 4242)
(def url (str "http://localhost:" port "/"))
(defn html-str
  ([input] (str (html input)))
  ([input & rest] (str (html input) (->> rest (map #(html %)) (apply str)))))

(defn app [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body (html-str
          [:html
           [:head
            [:link {:rel "stylesheet" :href "./css/style.css"}]
            [:script {:src "https://unpkg.com/htmx.org@1.8.4"}]
            [:script {:src "https://unpkg.com/htmx.org@1.8.4/dist/ext/ws.js"}]]
           [:body
            [:div {:class "h-screen bg-neutral-900"}
             [:button {:class "text-slate-100"
                       :hx-post "/clicked"
                       :hx-swap "outerHTML"}
              "Click Me"]
             [:div {:hx-ext "ws" :ws-connect "/ws"}
              [:div {:id "chats"
                     :class "h-fit text-slate-100"}
               "..."]
              [:form {:id "form" :ws-send "true"}
               [:input {:name "chat_message"}]]]]]])})

(defn clicked [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body (html-str [:h1 {:class "text-2xl text-slate-100"}
                    "BOO!"])})

;; from httpkit as-channel sourcecode
(defn websocket [req]
  (if (:websocket? req)
    (as-channel req
                {:on-open    (fn [ch]         (println "on-open:"    ch))
                 :on-close   (fn [ch status]  (println "on-close:"   status))
                 :on-receive (fn [ch message]
                               (println "on-receive:" message)
                               (send! ch {:body (html-str [:form {:id "form" :ws-send "true"}
                                                           [:input {:name "chat_message"}]]
                                                          [:div {:id "chats" :hx-swap-oob "beforeend"}
                                                           [:div {} (let [json-map (json/parse-string message)]
                                                                      (get json-map "chat_message" "hmm??"))]])}))})
    {:status 200 :body "<h1>Hmm?</h1>"}))

;; https://gist.github.com/borkdude/1627f39d072ea05557a324faf5054cf3
(defn router [req]
  (let [paths (vec (rest (str/split (:uri req) #"/")))]
    (match/match [(:request-method req) paths]
      [:get []] (app req)
      [:get ["ws"]] (websocket req)
      [:post ["clicked"]] (clicked req)
      [:get ["css" "style.css"]] {:body (slurp "resources/public/css/style.css")}
      :else {:body "<p>Page not found.</p>"})))

;; https://http-kit.github.io/server.html#stop-server
(defonce server (atom nil))

(defn stop-bantuin []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn start-bantuin []
  (reset! server (run-server #'router {:port port})))