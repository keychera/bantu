(ns funrepo.anki
  (:require [bantu.common :refer [http-get]]))

;; https://foosoft.net/projects/anki-connect/
(defonce anki-connect "localhost:8765")

(defn connect []
  (http-get anki-connect
             {:raw-args ["--retry-all-errors"
                         "--retry" "1"
                         "--retry-delay" "0"]}))


