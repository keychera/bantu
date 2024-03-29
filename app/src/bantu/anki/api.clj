(ns bantu.anki.api
  (:require [babashka.curl :as curl]
            [babashka.process :refer [shell]]
            [cheshire.core :as json]
            [clojure.core.async :refer [alt! chan close! thread timeout]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [org.httpkit.server :refer [as-channel send!]]
            [selmer.parser :refer [render-file]]))

(defn ^{:sidebar "anki" :title "Anki"} anki [] (render-file "bantu/anki/anki.html" {}))

;; https://foosoft.net/projects/anki-connect/
(defonce anki-connect "localhost:8765")

(defn connect []
  (curl/get anki-connect
            {:compressed false
             :raw-args ["--retry-all-errors"
                        "--retry" "1"
                        "--retry-delay" "0"]}))

(defn connect-anki [_]
  (try (let [response (connect)]
         {:body (render-file "bantu/anki/success.html"
                             {:anki-connect-ver (:body response)})})
       (catch Exception e
         {:body (render-file "bantu/anki/failed.html"
                             {:reason (-> e Throwable->map :cause)})})))
(defn query [input]
  (try (-> (curl/get anki-connect
                     {:headers {"Content-Type" "application/json; charset=utf-8"}
                      :body (json/generate-string
                             {:action "findNotes"
                              :params {:query (str "deck:current " input)}})
                      :compressed false})
           :body edn/read-string)
       (catch Throwable e (println "[anki][ERROR]" (-> e Throwable->map :cause)))))

(defn count-unit [number unit]
  (str number " " unit (when-not (= number 1) "s")))

(defn relevant-search-html [sanitized-word]
  (let [word-query (str "word:*" sanitized-word "*")
        word-search (some-> (query word-query) count)
        any-search (some-> (query sanitized-word) count)]
    (render-file "bantu/anki/result.html" {:word-query word-query
                                           :word-field (count-unit (or word-search 0) "card")
                                           :any-query sanitized-word
                                           :any-field (count-unit (or any-search 0) "card")})))

(defonce clipboard-watcher (atom nil))

(def clip-cmd (condp #(str/includes? (some->> %2 (remove #{\ }) (apply str) str/lower-case) %1) (System/getProperty "os.name")
                "macos" "pbpaste"
                ;; in Windows, need to enable beta setting to make system use UTF-8
                "windows" "powershell clipboard"))

(defn read-clipboard []
  (try (-> (shell {:out :string} clip-cmd) :out)
       (catch Throwable e (println "[anki][ERROR]" (-> e Throwable->map :cause)))))

(defn sanitize [word]
  (->> word str/trim str/trim-newline (remove #{\" \' \: \; \{ \} \( \) \[ \] \/ \\}) (apply str)))

(defn watch-clipboard [ws-ch]
  (println "watching clipboard...")
  (let [exit-ch (chan)]
    (thread
      (loop [last-clip :begin t-ch (timeout 200)]
        (alt!
          exit-ch (println "stopped watching clipboard...")
          t-ch (let [clip (-> (read-clipboard) sanitize)
                     size (count clip)]
                 (when (and (not= last-clip :begin) (and (> size 0) (< size 64)) (not= clip last-clip))
                   (println "searching clip =>" clip)
                   (send! ws-ch {:body (str/join [(render-file "bantu/anki/input.html" {:value clip})
                                                  (relevant-search-html clip)])}))
                 (recur clip (timeout 200))))))
    exit-ch))


(defn anki-ws [req]
  (if (:websocket? req)
    (as-channel req
                {:on-open  (fn [ch]
                             (println "[anki] on-open")
                             (let [[prev-watcher _] (reset-vals! clipboard-watcher (watch-clipboard ch))]
                               (some-> prev-watcher close!)))
                 :on-close (fn [_ status]
                             (println "[anki] on-close" status)
                             (let [[prev-watcher _] (reset-vals! clipboard-watcher nil)]
                               (some-> prev-watcher close!)))})
    {:status 200 :body "<h1>tidak anki disini</h1>"}))

(defn search [req]
  (let [payload (some-> req :body (io/reader :encoding "UTF-8") slurp)
        search-query (some-> payload (str/split #"=") ;; assuming only one param
                             (some->> (map #(java.net.URLDecoder/decode %)) last))]
    (println "responding query => " search-query)
    (when-not (nil? search-query)
      {:body (relevant-search-html search-query)})))


(defn gui-query [input]
  (try (-> (curl/get anki-connect
                     {:headers {"Content-Type" "application/json; charset=utf-8"}
                      :body (json/generate-string
                             {:action "guiBrowse"
                              :version 6
                              :params {:query (str "deck:current " input)}})
                      :compressed false}))
       (catch Throwable e (println "[anki][ERROR]" (-> e Throwable->map)))))

(defn search-gui [req]
  (let [payload (some-> req :body (io/reader :encoding "UTF-8") slurp)
        search-query (some-> payload (str/split #"=") ;; assuming only one param, param name is ignored
                             (some->> (map #(java.net.URLDecoder/decode %)) last))]
    (gui-query search-query)))

(comment
  (query "ナヒーダ")
  (gui-query "ナヒーダ"))
