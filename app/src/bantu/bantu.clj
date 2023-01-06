(ns bantu.bantu
  (:require [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [funrepo.anki :as anki]
            [selmer.parser :refer [render-file]]))

(defn connect-anki []
  (let [anki-response (try (anki/connect) (catch Exception _ nil))]
    (if anki-response
      {:status 200
       :body (render-file "connect-success.html" {:anki-connect-ver (:body anki-response)})}
      {:status 200
       :body (render-file "connect-failed.html" {})})))

(defn app
  [main opts] (render-file "ex/bread.html" (merge {:render-main main} opts)))

;; components
(defn hello [] {:sidebar "hello" :body "hello"})
(defn doc [] {:sidebar "doc" :body (render-file "ex/doc.html" {})})
(defn anki [] {:sidebar "anki" :body (render-file "ex/anki.html" {})})
(defn intro [] {:sidebar "doc" :body "intro"})
(defn user [id] {:body (str "user is " id)})

;; sidebars
(defmacro sidebar-maps [& syms]
  (zipmap (map (comp name :handler) syms) syms))

(def sidebars (sidebar-maps
               {:handler hello :text "Hello!"}
               {:handler doc :text "Doc"}
               {:handler anki :text "Anki"}))

(defn render-sidebars [selected]
  (->> sidebars
       (map (fn [[name {:keys [text]}]]
              (render-file "ex/sidebar.html" {:url name :text text :selected (= name selected)})))
       (reduce str)))

(defn part? [req] (= "p" (:query-string req)))

(defn component-route [partial component]
  (if partial component
      (let [{:keys [sidebar body]} component]
        {:body (app body {:render-sidebars (render-sidebars sidebar)})})))

(defn router [req]
  (let [paths (-> (:uri req) (str/split #"/") rest vec)
        verb (:request-method req)
        route (partial component-route (part? req))]
    (match [verb paths]
      [:get []] {:body (app nil {:render-sidebars (render-sidebars nil)})}
      [:get ["hello"]] (route (hello))
      [:get ["doc"]] (route (doc))
      [:get ["anki"]] (route (anki))
      [:get ["doc" "intro"]] (route (intro))
      [:get ["user" id]] (route (user id))
      
      [:post ["connect-anki"]] {:as-async connect-anki}
      
      [:get ["css" "style.css"]] {:body (slurp (io/resource "public/css/style.css"))}
      :else {:status 404 :body "not found"})))