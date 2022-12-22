(ns panas.reload
  (:require [bantu.bantu :as bantu]
            [bantu.common :refer [from-here]]
            [clojure.core.match :as match]
            [clojure.string :as str]
            [org.httpkit.server :refer [as-channel run-server send!]]
            [pod.babashka.filewatcher :as fw]
            [pod.retrogradeorbit.bootleg.utils :as utils]))

;; https://http-kit.github.io/server.html#stop-server
;; https://cognitect.com/blog/2013/06/04/clojure-workflow-reloaded
(defonce port 4242)
(defonce url (str "http://localhost:" port "/"))
(defonce server (atom nil))

(defn panas-websocket [embedded-server req]
  (if (:websocket? req)
    (as-channel req
                {:on-open  (fn [ch]
                             (println "[panas] on-open")
                             (send! ch
                                    {:body (let [response (embedded-server {:uri "" :request-method :get})
                                                 html-body (-> response :body)]
                                              ;; assuming always root url for reloading which contains html>body
                                             (-> (utils/convert-to html-body :hickory)
                                                 (as-> {[_ {:keys [attrs] :as akar-body}] :content}
                                                       (assoc akar-body :attrs (assoc attrs :id "akar" :hx-swap-oob "innerHtml")))
                                                 (assoc :tag :div)
                                                 (utils/convert-to :html)))}))
                 :on-close (fn [_ status] (println "[panas] on-close" status))})
    {:status 200 :body "<h1>tidak panas disini</h1>"}))

;; https://clojuredocs.org/clojure.core/empty_q
(defn not-empty? [coll] (seq coll))

(defn with-htmx-ws [head]
  (let [content (:content head)
        scripts (->> content (filter #(= (:tag %) :script)))
        htmx-ws? (->> scripts (map :attrs) (map :src) (filter #(str/includes? % "dist/ext/ws.js")) not-empty?)]
    (if htmx-ws? head
        (assoc head :content (conj content {:type :element :attrs {:src "https://unpkg.com/htmx.org@1.8.4/dist/ext/ws.js"} :tag :script :content nil})))))

(defn with-akar [server req]
  (as-> (server req) res
    (let [{html-res :body} res
          hick-res (utils/convert-to html-res :hickory)]
      (cond (= (:tag hick-res) :html)
            (-> hick-res
                (as-> {[head {:keys [attrs] :as body}] :content :as html}
                      (assoc html :content
                             [(with-htmx-ws head) 
                              (assoc body :attrs
                                          (assoc attrs :id "akar" :hx-ext "ws" :ws-connect "/panas"))]))
                (utils/convert-to :html)
                (->> (assoc res :body)))
            :else res))))

(defn panas-reload [embedded-server req]
  (let [paths (vec (rest (str/split (:uri req) #"/")))]
    (match/match [(:request-method req) paths]
      [:get ["panas"]] (panas-websocket embedded-server req)
      :else (if (:websocket? req)
              (embedded-server req)
              (with-akar embedded-server req)))))

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