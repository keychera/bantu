(ns bantu.anki.api
  (:require [babashka.curl :as curl]
            [babashka.process :refer [shell]]
            [cheshire.core :as json]
            [clojure.core.async :refer [alt! chan close! thread timeout]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [org.httpkit.server :refer [as-channel send!]]
            [selmer.parser :refer [render render-file]]))

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
  (let [sanitized (->> input (str/trim) (str/trim-newline)
                       (remove #{\" \' \: \; \{ \} \( \) \[ \] \/ \\}) (apply str))]
    (-> (curl/get anki-connect
                  {:headers {"Content-Type" "application/json; charset=utf-8"}
                   :body (json/generate-string
                          {:action "findNotes"
                           :params {:query (str "deck:current " sanitized)}})
                   :compressed false})
        :body edn/read-string)))

(defn count-unit [number unit]
  (str number " " unit (when-not (= number 1) "s")))

(defn relevant-search-html [word]
  (let [word (-> word str/trim-newline)
        word-search (-> (render "word:*{{word}}*" {:word word}) query count (count-unit "card"))
        any-search (-> word query count (count-unit "card"))]
    (render-file "bantu/anki/result.html" {:word-field word-search
                                           :any-field any-search})))

(defonce clipboard-watcher (atom nil))

(def clip-cmd (condp #(str/includes? (some->> %2 (remove #{\ }) (apply str) str/lower-case) %1) (System/getProperty "os.name")
                "macos" "pbpaste"
                ;; in Windows, need to enable beta setting to make system use UTF-8
                "windows" "powershell clipboard"))

(defn read-clipboard []
  (-> (shell {:out :string} clip-cmd) :out))

(defn watch-clipboard [ws-ch]
  (println "watching clipboard...")
  (let [exit-ch (chan)]
    (thread
      (loop [i 0 last-clip (read-clipboard) t-ch (timeout 200)]
        (alt!
          exit-ch (println "stopped watching clipboard...")
          t-ch (let [clip (read-clipboard)]
                 (when-not (= clip last-clip)
                   (println "searching clip =>" clip)
                   (send! ws-ch {:body (str/join [(render-file "bantu/anki/input.html" {:value clip})
                                                  (relevant-search-html clip)])}))
                 (recur (inc i) clip (timeout 200))))))
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

(defn anki-search [req]
  (let [payload (some-> req :body (io/reader :encoding "UTF-8") slurp)
        search-query (some-> payload (str/split #"=") ;; assuming only one param
                             (some->> (map #(java.net.URLDecoder/decode %)) last))]
    (println "responding query => " search-query)
    (when-not (nil? search-query)
      {:body (relevant-search-html search-query)})))
