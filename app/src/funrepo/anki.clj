(ns funrepo.anki
  (:require [babashka.curl :as curl]
            [cheshire.core :as json]
            [clojure.pprint :refer [pprint]]))

;; https://foosoft.net/projects/anki-connect/
(defn find [word]
  (curl/post "localhost:8765"
             {:body (json/generate-string
                     {:action "findCards" :version 6
                      :params {:query (str "word:*" word "*")}})}))

(defn -main [& args]
  (pprint (find "あっという間")))

