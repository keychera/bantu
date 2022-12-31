(ns bantu.bantu
  (:require [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [funrepo.anki :as anki]
            [org.httpkit.server :refer [as-channel send!]]
            [selmer.parser :refer [render-file]]))

(defn connect-anki []
  (let [anki-response (try (anki/connect) (catch Exception _ nil))]
    (if anki-response
      {:status 200
       :body (render-file "connect-success.html" {:anki-connect-ver (:body anki-response)})}
      {:status 200
       :body (render-file "connect-failed.html" {})})))

;; from as-channel source code docs
(defn anki-async-handler [ring-req]
  (as-channel ring-req {:on-open (fn [ch]  (send! ch (connect-anki)))}))

;; ;; https://gist.github.com/borkdude/1627f39d072ea05557a324faf5054cf3
;; (defn router [req]
;;   (let [paths (vec (rest (str/split (:uri req) #"/")))]
;;     (match [(:request-method req) paths]
;;       [:get []] (app req)
;;       [:get ["connect-anki"]] (anki-async-handler req)
;;       [:get ["css" "style.css"]] {:body (slurp (io/resource "public/css/style.css"))}
;;       :else {:status 404 :body "<p>Page not found.</p>"})))


(defn app
  [main opts] (render-file "ex/bread.html" (merge {:render-main main} opts)))

;; components
(defn hello [] {:sidebar "hello" :body "hello"})
(defn doc [] {:sidebar "doc" :body (render-file "ex/doc.html" {})})
(defn intro [] {:sidebar "doc" :body "intro"})
(defn user [id] {:body (str "user is " id)})

;; sidebars
(defmacro sidebar-maps [& syms]
  (zipmap (map (comp name :handler) syms) syms))

(def sidebars (sidebar-maps
               {:handler hello :text "Hello!"}
               {:handler doc :text "Doc"}))

(defn render-sidebars [selected]
  (->> sidebars
       (map (fn [[k {:keys [text]}]]
              (render-file "ex/sidebar.html" {:url k :text text :selected (:sidebar selected)})))
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
      [:get ["doc" "intro"]] (route (intro))
      [:get ["user" id]] (route (user id))
      [:get ["css" "style.css"]] {:body (slurp (io/resource "public/css/style.css"))}
      :else {:status 404 :body "not found"})))