#!/usr/bin/env nbb
(ns get-task-info
  "Get task information via API"
  (:require [nbb.core :as nbb]
            [promesa.core :as p]))

(def api-host (or (.-LOGSEQ_API_HOST js/process.env) "localhost"))

(defn api-call [method args]
  (p/let [response (js/fetch (str "http://" api-host ":12315/api")
                             #js {:method "POST"
                                  :headers #js {"Content-Type" "application/json"
                                              "Authorization" "Bearer whatever"}
                                  :body (js/JSON.stringify
                                         #js {:method method
                                              :args (clj->js args)})})
          data (.json response)]
    (js->clj data :keywordize-keys true)))

(defn get-block [uuid]
  (p/let [result (api-call "logseq.Editor.getBlock" [uuid])]
    result))

(defn get-property [uuid prop-name]
  (p/let [result (api-call "logseq.Editor.getBlockProperty" [uuid prop-name])]
    result))

(defn -main [args]
  (let [[uuid] args
        _ (when-not uuid
            (println "Usage: get_task_info.cljs <block-uuid>")
            (js/process.exit 1))]

    (p/let [block (get-block uuid)
            status (get-property uuid "logseq.property/status")
            priority (get-property uuid "logseq.property/priority")]
      (if (:error block)
        (do
          (println "Error:" (:error block))
          (js/process.exit 1))
        (do
          (println "\n=== Task Information ===")
          (println "UUID:" uuid)
          (println "Content:" (:content block))
          (println "Status:" status)
          (println "Priority:" priority)
          (println "\nFull block data:")
          (println (js/JSON.stringify (clj->js block) nil 2)))))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
