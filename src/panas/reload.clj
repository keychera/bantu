(ns panas.reload
  (:require [bantu.bantu :as bantu]
            [bantu.common :refer [from-here]]
            [clojure.core.match :as match]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [org.httpkit.server :refer [as-channel run-server send!]]
            [pod.babashka.filewatcher :as fw]
            [pod.retrogradeorbit.bootleg.utils :as utils]
            [pod.retrogradeorbit.hickory.select :as s]))

;; https://http-kit.github.io/server.html#stop-server
;; https://cognitect.com/blog/2013/06/04/clojure-workflow-reloaded
(defonce port 4242)
(defonce url (str "http://localhost:" port "/"))
(defonce server (atom nil))
(defonce panas-ch (atom nil))

(defn refresh-body! [embedded-server]
  (send! @panas-ch {:body (let [res (embedded-server {:uri "" :request-method :get}) ;; assuming root url which respinse is html>body
                                hick-res (utils/convert-to (:body res) :hickory)
                                [{:keys [attrs] :as body}] (s/select (s/child (s/tag :body)) hick-res)] 
                            (as-> body akar-body
                              (assoc akar-body :attrs (assoc attrs :id "akar" :hx-swap-oob "innerHtml"))
                              (assoc akar-body :tag :div)
                              (utils/convert-to akar-body :html)))}))

(defn panas-websocket [req]
  (if (:websocket? req)
    (as-channel req
                {:on-open  (fn [ch]
                             (println "[panas] on-open")
                             (reset! panas-ch ch))
                 :on-close (fn [_ status]
                             (println "[panas] on-close" status)
                             (reset! panas-ch nil))})
    {:status 200 :body "<h1>tidak panas disini</h1>"}))

;; https://clojuredocs.org/clojure.core/empty_q
(defn not-empty? [coll] (seq coll))

(defn with-htmx-ws [head]
  (let [content (:content head)
        scripts (->> content (filter #(= (:tag %) :script)))
        htmx-ws? (->> scripts (map :attrs) (map :src) (filter #(str/includes? % "dist/ext/ws.js")) not-empty?)]
    (if htmx-ws? head
        (assoc head :content (conj content {:type :element :attrs {:src "https://unpkg.com/htmx.org@1.8.4/dist/ext/ws.js"} :tag :script :content nil})))))

(defn with-akar [response]
  (let [hick-seq (utils/convert-to (:body response) :hickory-seq)
        html? (->> hick-seq (map :tag) (filter #(= % :html)) not-empty?)]
    (if-not html? response
            (let [;; conj with "\n"  ensure partition-by results in at least three element, destructuring [_ front] ignores it back
                  [[_ & front] [html] & rest] (partition-by #(= (:tag %) :html) (-> hick-seq (conj "\n")))
                  [[_ & body-front] [body] & body-rest] (partition-by #(= (:tag %) :body) (-> (:content html) seq (conj "\n")))
                  [[_ & head-front] [head] & head-rest] (partition-by #(= (:tag %) :head) (-> body-front (conj "\n")))
                  akar-head (with-htmx-ws head)
                  akar-body (assoc body :attrs {:id "akar" :hx-ext "ws" :ws-connect "/panas"})
                  akar-html (assoc html :content (->> [head-front akar-head head-rest akar-body body-rest] (remove nil?) flatten vec))
                  akar-seq (->> [front akar-html rest] (remove nil?) flatten seq)]
              (assoc response :body (utils/convert-to akar-seq :html))))))

(defn panas-reload [embedded-server req]
  (let [paths (vec (rest (str/split (:uri req) #"/")))]
    (match/match [(:request-method req) paths]
      [:get ["panas"]] (panas-websocket req)
      :else (let [res (embedded-server req)]
              (if (:websocket? req) res
                  (with-akar res))))))

(defn start-panasin [server-to-embed]
  (let [to-embed (partial panas-reload server-to-embed)]
    (reset! server (run-server #'to-embed {:port port}))))

(def bantu-dir (from-here "../bantu"))

(defn -main [& _]
  (let [bantu-server bantu/router]
    (println "[panas] starting")
    (start-panasin bantu-server)
    (fw/watch bantu-dir
              (fn [event]
                (when (= :write (:type event))
                  (println "======")
                  (try
                    (let [file-to-reload (:path event)]
                      (println "[panas] reloading" file-to-reload)
                      (load-file file-to-reload))
                    (println "[panas] refreshing" url)
                    (refresh-body! bantu-server)
                    (catch Exception e
                      (println "[panas][error]" (with-out-str (pprint e)))))))
              {:delay-ms 50})
    (println "[panas] serving" url)
    @(promise)))