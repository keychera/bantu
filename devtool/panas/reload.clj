(ns panas.reload
  (:require [bantu.bantu :as bantu]
            [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [org.httpkit.server :refer [as-channel run-server send!]]
            [babashka.pods :as pods]
            [pod.retrogradeorbit.bootleg.utils :as utils]
            [pod.retrogradeorbit.hickory.select :as s]
            [serve :refer [async-wrapper]]))

(pods/load-pod 'org.babashka/filewatcher "0.0.1")
(require '[pod.babashka.filewatcher :as fw])

;; https://http-kit.github.io/server.html#stop-server
;; https://cognitect.com/blog/2013/06/04/clojure-workflow-reloaded
(defonce port 4242)
(defonce url (str "http://localhost:" port))
(defonce panas-ch (atom nil))
(defonce current-url (atom "/"))

(defn refresh-body! [embedded-server ch]
  (println "[panas] refreshing" (str url @current-url))
  (if (nil? ch) (println "[panas][warn] no opened panas client!")
      (send! ch {:body (let [res (embedded-server {:request-method :get :uri @current-url}) ;; assuming :get url which response is html>body
                             hick-res (utils/convert-to (:body res) :hickory)
                             [{:keys [attrs] :as body}] (s/select (s/child (s/tag :body)) hick-res)]
                         (-> body
                             (assoc :attrs (assoc attrs :id "akar" :hx-swap-oob "innerHtml"))
                             (assoc :tag :div)
                             (utils/convert-to :html)))})))

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

(def css-refresher-js {:type :element :attrs nil :tag :script
                       :content [(slurp (io/resource "panas/cssRefresher.js"))]})

(defn with-akar [response]
  (let [hick-seq (utils/convert-to (:body response) :hickory-seq)
        html? (->> hick-seq (map :tag) (filter #(= % :html)) not-empty?)]
    (if-not html? response
            (let [;; conj with "\n"  ensure partition-by results in at least three element, destructuring [_ front] ignores it back
                  ;; TODO bug: this transformation causes emoji unicode to break
                  [[_ & front] [html] & rest] (partition-by #(= (:tag %) :html) (-> hick-seq (conj "\n")))
                  [[_ & body-front] [body] & body-rest] (partition-by #(= (:tag %) :body) (-> (:content html) seq (conj "\n")))
                  [[_ & head-front] [head] & head-rest] (partition-by #(= (:tag %) :head) (-> body-front (conj "\n")))
                  akar-head (with-htmx-ws head)
                  akar-body (assoc body
                                   :attrs (merge (:attrs body) {:id "akar" :hx-ext "ws" :ws-connect "/panas"})
                                   :content (conj (:content body) css-refresher-js))
                  akar-html (assoc html :content (->> [head-front akar-head head-rest akar-body body-rest] (remove nil?) flatten vec))
                  akar-seq (->> [front akar-html rest] (remove nil?) flatten seq)]
              (assoc response :body (utils/convert-to akar-seq :html))))))

(defn panas-reload [embedded-server req]
  (let [uri (:uri req) ;; probably need better way to detect reloadable url
        verb (:request-method req)
        paths (vec (rest (str/split uri #"/")))]
    (when (and (= verb :get)
               (not (:websocket? req))
               (not (str/starts-with? uri "/css"))
               (not (str/starts-with? uri "/favicon.ico")))
      (reset! current-url uri)
      (println "currently on" uri))
    (match [verb paths]
      [:get ["panas"]] (panas-websocket req)
      :else (let [res (embedded-server req)]
              (cond (:websocket? req) res
                    (= (:async-channel req) (:body res)) res
                    :else (with-akar res))))))

(defn start-panasin [server-to-embed]
  (let [to-embed (partial panas-reload server-to-embed)]
    (run-server to-embed {:port port :thread 12})))

(def app-dir (-> (io/resource "pivot") .getPath (str/split #"/") drop-last drop-last
                 (->> (reduce #(str %1 "/" %2)))
                 (str "/app")))

(defn -main [& _]
  ;; the symbol #' is still mysterious, without that, hot reload doesn't work on router changes
  (let [router (partial #'async-wrapper #'bantu/router)]
    (println "[panas] starting")
    (start-panasin router)
    (fw/watch app-dir
              (fn [event]
                (when (= :write (:type event))
                  (println "======")
                  (try
                    (let [changed-file (:path event)]
                      (cond
                        (str/ends-with? changed-file ".clj") (do (println "[panas][clj] reloading" changed-file)
                                                                 (load-file changed-file))
                        (str/ends-with? changed-file ".html") (println "[panas][html] changes on" changed-file)
                        :else (println "[panas][other] changes on" changed-file))) 
                    (refresh-body! router @panas-ch)
                    (catch Exception e
                      (let [{:keys [cause]} (Throwable->map e)]
                        (println) (println "[panas][ERROR]" cause) (println))))))
              {:delay-ms 100})
    (println "[panas] serving" url)
    @(promise)))