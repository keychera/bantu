(ns bantu.fn-ui
  (:require [selmer.parser :refer [render-file]]
            [clojure.string :as str]))

(defn fn-list-ui [namespace]
  (require namespace)
  (let [fns (->> (vals (ns-publics 'funrepo.fns))
                 (map #(merge {:ref %} (meta %))) (filter :bantu)
                 (map #(select-keys % [:ref :name])))]
    (render-file "fn/list.html" {:fns fns})))

(defn fn-ref [ref-string]
  (let [fn-ref (-> ref-string read-string)
        ns-symbol (-> (re-matches #"#('.*)\/.*" "#'funrepo.fns/retrieve") (get 1) read-string)]
    (require (eval ns-symbol))
    (meta (eval fn-ref))))

(defn arglist->ui [arglists]
  (->> arglists
       (map #(merge {:arg %} (meta %)))
       (map #(if (:values %) (update % :values (comp var-get eval)) %))
       (map #(render-file "fn/input.html" {:type (:type %)
                                           :id (:arg %)
                                           :placeholder (:arg %)}))
       (str/join "\n")))

(defn fn-ui [{name :name [arglists] :arglists}]
  (let [input-ui (arglist->ui arglists)]
    (render-file "fn/ui.html" {:name name
                               :input-ui input-ui})))

(comment
  (fn-list-ui 'funrepo.fns)
  (-> (fn-ref "#'funrepo.fns/retrieve") fn-ui))