(ns bantu.bantu
  (:require [bantu.anki.api :as anki]
            [bantu.fn.ui :refer [execute-fn fn-list-ui fn-page fn-ui]]
            [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [selmer.parser :refer [render-file]]))

(defn app
  [main opts] (render-file "bantu/app.html" (merge {:render-main main} opts)))

;; components
(defn ^{:sidebar "hello" :title "Hello"} hello [] "hello")

;; engine
(defn render-sidebars [selected]
  (->> [#'hello #'anki/anki #'fn-page]
       (map (fn [handler]
              (let [{:keys [sidebar url title]} (meta handler)]
                (render-file "bantu/sidebar.html" {:url (or url sidebar) :title title :selected (= sidebar selected)}))))
       (reduce str)))

(defn part? [req] (= "p" (:query-string req)))

(defn sidebar-route [partial? handler & args]
  (if partial? {:body (apply handler args)}
      (let [{:keys [sidebar]} (meta handler)]
        {:body (app (apply handler args) {:render-sidebars (render-sidebars sidebar)})})))

(defn app-router [req]
  (let [paths (some-> (:uri req) (str/split #"/") rest vec)
        verb (:request-method req)
        route (partial sidebar-route (part? req))]
    (match [verb paths]
      [:get []] {:body (app nil {:render-sidebars (render-sidebars nil)})}
      [:get ["hello"]] (route #'hello)

      [:get ["hmm"]] {:body "読み"}
      [:get ["anki"]] (route #'anki/anki)
      [:get ["anki-ws"]] (anki/anki-ws req)

      [:get ["fn" ns-val]] (route #'fn-list-ui (symbol ns-val))
      [:get ["fn" ns-val name]] (route #'fn-ui (str "#'" ns-val "/" name))
      [:post ["fn" ns-val name]] (execute-fn req ns-val name)

      [:post ["connect-anki"]] (anki/connect-anki req)
      [:post ["anki-search"]] (anki/search req)
      [:post ["anki-search-gui"]] (anki/search-gui req)

      [:get ["css" "style.css"]] {:headers {"Content-Type" "text/css"}
                                  :body (slurp (io/resource "public/css/style.css"))}
      :else {:status 404 :body "not found"})))

(defn wrap-content-type [handler]
  (fn [req]
    (let [res (handler req)]
      (update res :headers
              #(if (contains? % "Content-Type") %
                   (assoc % "Content-Type" "text/html; charset=utf-8"))))))

(def router (-> app-router
                wrap-content-type))