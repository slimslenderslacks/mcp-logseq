#!/usr/bin/env nbb
(ns list-page-tasks-api
  "List tasks on a page with their properties via API"
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

(defn get-block-property [uuid property-name]
  (p/let [result (api-call "logseq.Editor.getBlockProperty"
                          [uuid property-name])]
    result))

(defn get-block-tags [uuid]
  (p/let [result (api-call "logseq.Editor.getAllTags"
                          [uuid])]
    result))

(defn list-blocks [page-name]
  (p/let [result (api-call "logseq.Editor.getPageBlocksTree" [page-name])]
    result))

(defn -main [args]
  (let [[page-name] args
        page-name (or page-name "Feb 5th, 2026")]

    (p/let [blocks (list-blocks page-name)]
      (if (:error blocks)
        (do
          (println "Error:" (:error blocks))
          (js/process.exit 1))
        (do
          (println "\n=== Checking blocks on" page-name "for tasks ===\n")

          ;; Check each block for task properties
          (p/loop [blocks-to-check blocks]
            (when-let [block (first blocks-to-check)]
              (p/let [uuid (:uuid block)
                      tags (get-block-tags uuid)
                      status (get-block-property uuid "logseq.property/status")
                      priority (get-block-property uuid "logseq.property/priority")]

                (when (and tags (some #(= "Task" %) tags))
                  (println "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                  (println "Task UUID:" uuid)
                  (println "  Content:" (:content block))
                  (println "  Status:" status)
                  (println "  Priority:" priority)
                  (println))

                (p/recur (rest blocks-to-check))))))))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
