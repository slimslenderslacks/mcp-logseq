#!/usr/bin/env nbb
(ns add-content
  "Add content to a page or as a child block via API"
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

(defn uuid? [s]
  "Check if string is a UUID (format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)"
  (and s (re-matches #"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$" s)))

(defn add-content [page-or-block-id content]
  (p/let [;; If page-or-block-id is a UUID, create as child block, otherwise create on page
          block-result (if (uuid? page-or-block-id)
                        ;; Create as child block
                        (api-call "logseq.Editor.insertBlock"
                                 [page-or-block-id content #js {:sibling false}])
                        ;; Create as top-level block on page
                        (api-call "logseq.Editor.appendBlockInPage"
                                 [page-or-block-id content]))]
    block-result))

(defn -main [args]
  (let [[page-or-block-id content] args
        _ (when-not (and page-or-block-id content)
            (println "Usage: add_content.cljs <page-or-block-id> <content>")
            (println "\nExamples:")
            (println "  Add content to a page:")
            (println "    add_content.cljs \"Feb 5th, 2026\" \"This is my note\"")
            (println "  Add child content to a block:")
            (println "    add_content.cljs \"69855f7c-2461-4158-98bd-b26434537654\" \"This is a child block\"")
            (println "\nNote: Supports markdown formatting in content")
            (js/process.exit 1))]

    (p/let [result (add-content page-or-block-id content)
            content-type (if (uuid? page-or-block-id) "Child block" "Top-level block")]
      (if (:error result)
        (do
          (println "Error:" (:error result))
          (js/process.exit 1))
        (do
          (println (str "\n✓ " content-type " created successfully!"))
          (println "  Block ID:" (:id result))
          (println "  UUID:" (:uuid result))
          (println "  Content:" content)
          (when (uuid? page-or-block-id)
            (println "  Parent UUID:" page-or-block-id))
          (println "\n✓ Content is immediately visible in Logseq!"))))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
