(ns bantu.bantu
  (:require [clojure.core.match :as match]
            [clojure.string :as str]
            [hiccup.core :refer [html]]
            [org.httpkit.server :refer [run-server]]))

(def port 4242)
(def url (str "http://localhost:" port "/"))

(defn app [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body (html
          [:html
           [:head
            [:link {:rel "stylesheet" :href "./css/style.css"}]
            [:script {:src "https://unpkg.com/htmx.org@1.8.4"}]]
           [:body
            [:div {:class "h-screen bg-neutral-900"}
             [:button {:class "text-slate-100"
                       :hx-post "/clicked"
                       :hx-swap "outerHTML"}
              "Click Me"]]]])})

(defn clicked [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body (html [:h1 {:class "text-2xl text-slate-100"}
                "BOO!"])})

;; https://gist.github.com/borkdude/1627f39d072ea05557a324faf5054cf3
(defn router [req]
  (let [paths (vec (rest (str/split (:uri req) #"/")))]
    (match/match [(:request-method req) paths]
      [:get []] (app req)
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