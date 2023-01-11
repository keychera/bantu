(ns bantu.fn-ui
  (:require [selmer.parser :refer [render-file]]))

(defn fn-list-ui [namespace]
  (require namespace)
  (let [fns (->> (vals (ns-publics 'funrepo.fns))
                 (map #(merge {:ref %} (meta %))) (filter :bantu)
                 (map #(select-keys % [:ref :name])))]
    (render-file "fn/list.html" {:fns fns})))

(defn fn-ui [name] name)

(comment
  (fn-list-ui 'funrepo.fns)
  (fn-ui "hello"))