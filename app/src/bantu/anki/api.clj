(ns bantu.anki.api
  (:require [babashka.curl :as curl]
            [selmer.parser :refer [render-file]]))

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

