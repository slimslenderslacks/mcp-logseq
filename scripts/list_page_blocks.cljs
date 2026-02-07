#!/usr/bin/env nbb
(ns list-page-blocks
  "List all blocks on a page via API"
  (:require [nbb.core :as nbb]
            [promesa.core :as p]))

(def api-host (or (.-LOGSEQ_API_HOST js/process.env) "host.docker.internal"))
(def api-port (or (.-LOGSEQ_API_PORT js/process.env) "12315"))
(def api-token (.-LOGSEQ_API_AUTHORIZATION_TOKEN js/process.env))

(defn api-call [method args]
  (p/let [headers (cond-> #js {"Content-Type" "application/json"}
                    api-token (doto (aset "Authorization" (str "Bearer " api-token))))
          response (js/fetch (str "http://" api-host ":" api-port "/api")
                             #js {:method "POST"
                                  :headers headers
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
