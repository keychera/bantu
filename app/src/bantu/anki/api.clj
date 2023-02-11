(ns bantu.anki.api 
  (:require [babashka.curl :as curl]
            [selmer.parser :refer [render-file]]))

;; https://foosoft.net/projects/anki-connect/
(defonce anki-connect "localhost:8765")

(defn connect []
  (curl/get anki-connect
            {:raw-args ["--retry-all-errors"
                        "--retry" "1"
                        "--retry-delay" "0"]}))

(defn connect-anki [_]
  (let [anki-response (try (connect) (catch Exception _ nil))]
    (if anki-response
      {:status 200
       :body (render-file "bantu/anki/success.html" {:anki-connect-ver (:body anki-response)})}
      {:status 200
       :body (render-file "bantu/anki/failed.html" {})})))

