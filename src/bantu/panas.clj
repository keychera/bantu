(ns bantu.panas
  (:require [bantu.bantu :as bantu]
            [clojure.core.match :as match]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [org.httpkit.server :refer [as-channel run-server]]
            [pod.retrogradeorbit.bootleg.utils :as utils]))

(defn panas-websocket [embedded-server req]
  (if (:websocket? req)
    (as-channel
     req
     {:on-open    (fn [ch]         (println "[panas] on-open    :" ch))
      :on-close   (fn [ch status]  (println "[panas] on-close   :" status))})
    {:status 200 :body "<h1>tidak panas disini</h1>"}))

(defn hick-to-embed [content]
  [{:type :element
    :attrs {:id "akar" :hx-ext "ws" :ws-connect "/panas"}
    :tag :div
    :content content}])

(defn embed-akar [hiccup-html embed-fn]
  (let [hick-html (utils/convert-to hiccup-html :hickory)
        [hick-head hick-body] (:content hick-html)
        hick-div-vectors (:content hick-body)]
    (pprint hick-html)
    (-> (assoc hick-body :content (embed-fn hick-div-vectors))
        (doto pprint)
        (as-> embedded-body (assoc hick-html :content [hick-head embedded-body]))
        (utils/convert-to :html))))

(defn panas-reload [embedded-server req]
  (let [paths (vec (rest (str/split (:uri req) #"/")))]
    (match/match [(:request-method req) paths]
      [:get ["panas"]] (panas-websocket embedded-server req) 
      :else (if (:websocket? req)
              (embedded-server req)
              (as-> (embedded-server req) it
                (let [hiccup-resp (-> it :body (utils/convert-to :hiccup))]
                  (if (some #{:html} hiccup-resp)
                    (-> (assoc it :body (embed-akar hiccup-resp hick-to-embed)))
                    it)))))))

(def port 4242)
(def url (str "http://localhost:" port "/"))
;; https://http-kit.github.io/server.html#stop-server
(defonce server (atom nil))

(defn start-panasin [server-to-embed]
  (reset! server (run-server #'server-to-embed {:port port})))

(defn stop-panasin []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn -main [& _]
  (println "[panas] starting")
  (start-panasin (partial panas-reload bantu/router))
  (println "[panas] serving" url)
  @(promise))