(ns funrepo.anki
  (:require [babashka.curl :as curl]
            [cheshire.core :as json]
            [clojure.pprint :refer [pprint]]))

;; https://foosoft.net/projects/anki-connect/
(defonce anki-connect "localhost:8765")

(defn connect []
  (curl/post anki-connect
             {:raw-args ["--retry-all-errors"
                         "--retry" "1"
                         "--retry-delay" "0"]}))


(defn -main [& args]
  (pprint (connect)))

