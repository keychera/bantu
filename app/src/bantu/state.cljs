(ns bantu.state)

(defn add-counter []
  (let [counter (.. js/document (getElementById "a-counter"))
        prev (js/parseInt (.. counter -textContent))]
    (-> (.. counter -textContent) (set! (+ prev 1)))))

(set! (.-add_counter js/window) add-counter)