(ns chera.panas
  (:require [bantu.bantu :as bantu]
            [bantu.common :refer [from-here html-str]]
            [clojure.core.match :as match]
            [clojure.string :as str]
            [org.httpkit.server :refer [as-channel run-server send!
                                        server-port]]
            [pod.babashka.filewatcher :as fw]
            [pod.retrogradeorbit.bootleg.utils :as utils]))

;; https://http-kit.github.io/server.html#stop-server
;; https://cognitect.com/blog/2013/06/04/clojure-workflow-reloaded
(defonce port 4242)
(defonce url (str "http://localhost:" port "/"))
(defonce server (atom nil))
(def panas-ch (atom nil))

(defn panas-websocket [embedded-server req]
  (if (:websocket? req)
    (as-channel
     req
     {:on-open    (fn [ch]
                    (println "[panas] on-open")
                    (reset! panas-ch ch)
                    (send! ch
                           {:body (html-str [:div {:id "akar" :hx-swap-oob "innerHtml"}
                                             (let [response (embedded-server {:uri "" :request-method :get})
                                                   hick (-> response :body (utils/convert-to :hickory))]
                                               (if (= (:tag hick) :html)
                                                 (-> hick :content (get 1) :content first (utils/convert-to :hiccup))
                                                 response))])}))
      :on-close   (fn [_ status]
                    (println "[panas] on-close" status)
                    (reset! panas-ch nil))})
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
    (-> (assoc hick-body :content (embed-fn hick-div-vectors))
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

(defn start-panasin [server-to-embed]
  (let [to-embed (partial panas-reload server-to-embed)]
    (reset! server (run-server #'to-embed {:port port}))))

(defn stop-panasin []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(def bantu-dir (from-here "../bantu"))

(defn -main [& _]
  (let [router bantu/router]
    (println "[panas] starting")
    (start-panasin router)
    (fw/watch bantu-dir
              (fn [event]
                (when (= :write (:type event))
                  (println "======")
                  (println "[panas] stopping" url) 
                  (try
                    (let [file-to-reload (:path event)]
                      (println "[panas] reloading" file-to-reload)
                      (load-file file-to-reload))
                    (stop-panasin)
                    (start-panasin router)
                    (println "[panas] serving" url)
                    (catch Exception e
                      (println "[panas][error]" (.getMessage e)))))))
    (println "[panas] serving" url)
    @(promise)))