(ns bantu.fn-ui
  (:require [selmer.parser :refer [render-file]]
            [clojure.string :as str]))

(defn ^{:sidebar "fn"} fn-list-ui [namespace]
  (require namespace)
  (let [fns (->> (vals (ns-publics 'funrepo.fns))
                 (map #(merge {:ref %} (meta %))) (filter :bantu)
                 (map #(select-keys % [:ref :name]))
                 (map #(merge % {:ns namespace})))]
    (render-file "fn/list.html" {:fns fns})))

(defn fn-ref [ref-string]
  (let [ref (-> ref-string read-string)
        ns-symbol (-> (re-matches #"#('.*)\/.*" ref-string) (get 1) read-string)]
    (require (eval ns-symbol))
    (meta (eval ref))))

(defn arglist->ui [arglists]
  (->> arglists
       (map #(merge {:arg %} (meta %)))
       (map #(render-file "fn/input.html" {:type (name (:type %))
                                           :id (:arg %) 
                                           :placeholder (:arg %)}))
       (str/join "\n")))

(defn ^{:sidebar "fn"} fn-ui [ref-string]
  (let [ref (fn-ref ref-string)
        ns-name (-> (re-matches #"#'(.*)\/.*" ref-string) (get 1))
        {name :name [arglists] :arglists} ref
        input-ui (arglist->ui arglists)]
    (render-file "fn/ui.html" {:name name
                               :post-url (str "/fn/" ns-name "/" name)
                               :input-ui input-ui})))

(comment
  (fn-list-ui 'funrepo.fns)
  (fn-ui "#'funrepo.fns/retrieve"))