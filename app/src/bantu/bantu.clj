(ns bantu.bantu
  (:require [bantu.fn-ui :refer [execute-fn fn-list-ui fn-ui]]
            [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [funrepo.anki :as anki]
            [selmer.parser :refer [render-file]]))

(defn connect-anki [_]
  (let [anki-response (try (anki/connect) (catch Exception _ nil))]
    (if anki-response
      {:status 200
       :body (render-file "anki/success.html" {:anki-connect-ver (:body anki-response)})}
      {:status 200
       :body (render-file "anki/failed.html" {})})))

(defn app
  [main opts] (render-file "app.html" (merge {:render-main main} opts)))

;; components
(defn ^{:sidebar "hello" :title "Hello"} hello [] "hello")
(defn ^{:sidebar "doc" :title "Doc"} doc [] (render-file "doc.html" {}))
(defn ^{:sidebar "anki" :title "Anki"} anki [] (render-file "anki/connect.html" {}))
(defn ^{:sidebar "doc"} intro [] "intro")
(defn ^{:sidebar "fn" :url "fn/funrepo.fns" :title "fn()"}
  fn-page [] (fn-list-ui 'funrepo.fns))

;; engine
(defn render-sidebars [selected]
  (->> [#'hello #'doc #'anki #'fn-page]
       (map (fn [handler]
              (let [{:keys [sidebar url title]} (meta handler)]
                (render-file "sidebar.html" {:url (or url sidebar) :title title :selected (= sidebar selected)}))))
       (reduce str)))

(defn part? [req] (= "p" (:query-string req)))

(defn sidebar-route [partial? handler & args]
  (if partial? {:body (apply handler args)}
      (let [{:keys [sidebar]} (meta handler)]
        {:body (app (apply handler args) {:render-sidebars (render-sidebars sidebar)})})))

(defn router [req]
  (let [paths (some-> (:uri req) (str/split #"/") rest vec)
        verb (:request-method req)
        route (partial sidebar-route (part? req))]
    (match [verb paths]
      [:get []] {:body (app nil {:render-sidebars (render-sidebars nil)})}
      [:get ["hello"]] (route #'hello)
      [:get ["doc"]] (route #'doc)
      [:get ["anki"]] (route #'anki)
      [:get ["doc" "intro"]] (route #'intro)
      
      [:get ["fn" ns-val]] (route #'fn-list-ui (symbol ns-val))
      [:get ["fn" ns-val name]] (route #'fn-ui (str "#'" ns-val "/" name))
      [:post ["fn" ns-val name]] (execute-fn req ns-val name)
      
      [:post ["connect-anki"]] (connect-anki req)

      [:get ["css" "style.css"]] {:body (slurp (io/resource "public/css/style.css"))}
      :else {:status 404 :body "not found"})))
