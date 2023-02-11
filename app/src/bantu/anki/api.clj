(ns bantu.anki.api
  (:require [babashka.curl :as curl]
            [clojure.core.async :refer [thread]]
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

(def anki-session (atom nil))

(defn clipboard-watcher []
  (thread
    (loop [i 0]
      (when-let [anki-session @anki-session]
        (send! anki-session 
               {:body (str "<div id='anki-counter'>" i "</div>")})
        (Thread/sleep 200)
        (recur (inc i))))))


(defn anki-ws [req]
  (if (:websocket? req)
    (as-channel req
                {:on-open  (fn [ch]
                             (println "[anki] on-open")
                             (let [[previous-session _] (reset-vals! anki-session ch)]
                               (when-not (nil? previous-session)
                                 (clipboard-watcher))))
                 :on-close (fn [_ status]
                             (println "[anki] on-close" status)
                             (reset! anki-session nil))})
    {:status 200 :body "<h1>tidak anki disini</h1>"}))