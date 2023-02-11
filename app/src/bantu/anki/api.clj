(ns bantu.anki.api
  (:require [babashka.curl :as curl]
            [babashka.process :refer [shell]]
            [clojure.core.async :refer [alt! chan close! thread timeout]]
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

(defonce anki-session (atom nil))
(defonce clipboard-watcher (atom nil))

(defn read-clipboard []
  (-> (shell {:out :byte} "powershell clipboard") :out
      (slurp :encoding "UTF-8")))

(defn watch-clipboard [ws-ch]
  (let [exit-ch (chan)]
    (thread
      (loop [i 0 last-clip (read-clipboard) t-ch (timeout 200)]
        (alt!
          exit-ch nil
          t-ch (let [clip (read-clipboard)]
                 (when-not (= clip last-clip)
                   (println "sending clip =>" (str clip))
                   (send! ws-ch
                          {:body (render-file "bantu/anki/input.html" {:value clip})}))
                 (recur (inc i) clip (timeout 200))))))
    exit-ch))


(defn anki-ws [req]
  (if (:websocket? req)
    (as-channel req
                {:on-open  (fn [ch]
                             (println "[anki] on-open")
                             (let [[prev-ws _] (reset-vals! anki-session ch)]
                               (when-not (and (nil? prev-ws) (nil? @clipboard-watcher))
                                 (reset! clipboard-watcher (watch-clipboard prev-ws)))))
                 :on-close (fn [_ status]
                             (println "[anki] on-close" status)
                             (reset! anki-session nil)
                             (let [[old-clip _] (reset-vals! clipboard-watcher nil)]
                               (some-> old-clip close!)))})
    {:status 200 :body "<h1>tidak anki disini</h1>"}))