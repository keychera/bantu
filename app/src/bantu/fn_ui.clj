(ns bantu.fn-ui
  (:require [selmer.parser :refer [render-file]]
            [clojure.string :as str]))

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
  (println arg-data)
  (render-file "fn/input/enum.html" arg-data))

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

(comment
  (fn-list-ui 'funrepo.fns)
  (fn-ui "#'funrepo.fns/retrieve"))