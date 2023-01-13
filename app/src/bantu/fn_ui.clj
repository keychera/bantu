(ns bantu.fn-ui
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [selmer.parser :refer [render-file]]))

(defn ^{:sidebar "fn"} fn-list-ui [namespace]
  (try
    (require namespace)
    (let [fns (->> (vals (ns-publics namespace))
                   (map #(merge {:ref %} (meta %))) (filter :bantu)
                   (map #(select-keys % [:ref :name]))
                   (map #(merge % {:ns namespace})))]
      (render-file "fn/list.html" {:fns fns}))
    (catch  Exception e
      (let [cause (-> e Throwable->map :cause)]
        (str "<p>" cause "</p>")))))

(defn- fn-ref [ns-symbol-str ref-string]
  (let [ref (read-string ref-string)
        ns-symbol (read-string ns-symbol-str)]
    (require (eval ns-symbol))
    (meta (eval ref))))

(defmulti arg->ui :type-key)
(defmethod arg->ui :default [arg-data]
  (render-file "fn/input/default.html" arg-data))

(defmethod arg->ui :checkbox [arg-data]
  (render-file "fn/input/checkbox.html" arg-data))

(defmethod arg->ui :enum [arg-data]
  (render-file "fn/input/enum.html" arg-data))

(defmethod arg->ui :file [arg-data]
  (render-file "fn/input/file.html" arg-data))

(defn- arglist->ui [arglists]
  (->> arglists
       (map #(let [data (meta %)
                   type-key (some-> data :type)
                   type (some-> type-key name)]
               {:data data :type-key type-key :type type :name % :placeholder %}))
       (map #(str (render-file "fn/input/hidden.html" %)
                  (arg->ui %)))
       (str/join "\n")))

(defn ^{:sidebar "fn"} fn-ui [ref-string]
  (try (let [ns-symbol-str (-> (re-matches #"#('.*)\/.*" ref-string) (get 1))
             ns-name (subs ns-symbol-str 1)
             ref (fn-ref ns-symbol-str ref-string)
             {name :name [arglists] :arglists} ref
             input-ui (arglist->ui arglists)]
         (render-file "fn/ui.html" {:name name
                                    :post-url (str "/fn/" ns-name "/" name)
                                    :input-ui input-ui}))
       (catch  Exception e
         (let [cause (-> e Throwable->map :cause)]
           (str "<p>" cause "</p>")))))

;; resolving empty inputs using this strategy https://stackoverflow.com/a/1992745/8812880
;; order sensitive following original fn parameters
(defn- resolve-empty [payload]
  (some-> payload (java.net.URLDecoder/decode) (str/split #"&")
          (->> (map #(str/split % #"="))
               (remove #(= (count %) 1))
               vec (into {}))
          (update-vals #(when-not (= % "--empty") %))))

(defn execute-fn [req ns-val name]
  (let [ref-string (str "#'" ns-val "/" name)
        fn-ref (-> ref-string read-string eval)
        payload (some-> req :body (io/reader :encoding "UTF-8") slurp)
        args (or (some-> payload resolve-empty vals) [])
        res (try (apply fn-ref args) (catch Exception e (-> e Throwable->map :cause)))]
    {:body (str "<p>" fn-ref "</p>"
                "<p>" res "</p>"
                "<p>" args "</p>")}))

(comment
  (fn-list-ui 'funrepo.fns)
  (fn-ui "#'funrepo.fns/retrieve"))