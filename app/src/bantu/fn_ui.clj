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

(defn- fn-ref [ns-symbol-str ref-string]
  (let [ref (read-string ref-string) 
        ns-symbol (read-string ns-symbol-str)]
    (require (eval ns-symbol))
    (meta (eval ref))))

(defn- arglist->ui [arglists]
  (->> arglists
       (map #(merge {:arg %} (meta %)))
       (map #(render-file "fn/input.html" {:type (some-> % :type name)
                                           :id (:arg %)
                                           :placeholder (:arg %)}))
       (str/join "\n")))

(defn ^{:sidebar "fn"} fn-ui [ref-string]
  (let [ns-symbol-str (-> (re-matches #"#('.*)\/.*" ref-string) (get 1))
        ns-name (subs ns-symbol-str 1)
        ref (fn-ref ns-symbol-str ref-string)
        {name :name [arglists] :arglists} ref
        input-ui (arglist->ui arglists)]
    (render-file "fn/ui.html" {:name name
                               :post-url (str "/fn/" ns-name "/" name)
                               :input-ui input-ui})))

(comment
  (fn-list-ui 'funrepo.fns)
  (fn-ui "#'funrepo.fns/retrieve"))