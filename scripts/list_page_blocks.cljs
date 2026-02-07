#!/usr/bin/env nbb
(ns list-page-blocks
  "List all blocks on a page via API"
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

(defn list-blocks [page-name]
  (p/let [result (api-call "logseq.Editor.getPageBlocksTree" [page-name])]
    result))

(defn -main [args]
  (let [[page-name] args
        page-name (or page-name "Feb 5th, 2026")
        _ (println "\n=== Blocks on" page-name "===\n")]

    (p/let [blocks (list-blocks page-name)]
      (if (:error blocks)
        (do
          (println "Error:" (:error blocks))
          (js/process.exit 1))
        (do
          (doseq [block blocks]
            (println "Block UUID:" (:uuid block))
            (println "  Content:" (:content block))
            (when-let [properties (:properties block)]
              (println "  Properties:" properties))
            (println))
          (println "\nTotal blocks:" (count blocks)))))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
