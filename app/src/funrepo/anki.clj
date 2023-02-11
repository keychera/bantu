(ns funrepo.anki
  (:require  [babashka.curl :as curl]))

;; https://foosoft.net/projects/anki-connect/
(defonce anki-connect "localhost:8765")

(defn connect []
  (curl/get anki-connect
   {:raw-args ["--retry-all-errors"
               "--retry" "1"
               "--retry-delay" "0"]}))


