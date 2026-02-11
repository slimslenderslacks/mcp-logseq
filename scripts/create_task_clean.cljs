#!/usr/bin/env nbb
(ns create-task-clean
  "Create a task via API with clean title (no visible #Task tag)"
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

(defn create-task-clean [page-or-block-id content status priority]
  (p/let [;; Step 1: Create block WITHOUT #Task in content
          ;; If page-or-block-id is a UUID, create as child block, otherwise create on page
          block-result (if (uuid? page-or-block-id)
                        ;; Create as sub-task/child block
                        (api-call "logseq.Editor.insertBlock"
                                 [page-or-block-id content #js {:sibling false}])
                        ;; Create as top-level task on page
                        (api-call "logseq.Editor.appendBlockInPage"
                                 [page-or-block-id content]))
          uuid (:uuid block-result)

          ;; Step 2: Add Task tag invisibly using addBlockTag
          _ (api-call "logseq.Editor.addBlockTag"
                     [uuid "Task"])

          ;; Step 3: Set status property
          _ (api-call "logseq.Editor.upsertBlockProperty"
                     [uuid "logseq.property/status" status])

          ;; Step 4: Set priority property
          _ (api-call "logseq.Editor.upsertBlockProperty"
                     [uuid "logseq.property/priority" priority])]
    block-result))

(defn -main [args]
  (let [[page-or-block-id content status priority] args
        status (or status "Todo")
        priority (or priority "High")
        _ (when-not (and page-or-block-id content)
            (println "Usage: create_task_clean.cljs <page-or-block-id> <content> [status] [priority]")
            (println "\nExamples:")
            (println "  Create top-level task:")
            (println "    create_task_clean.cljs \"Feb 5th, 2026\" \"My clean task\" Doing High")
            (println "  Create sub-task:")
            (println "    create_task_clean.cljs \"69855f7c-2461-4158-98bd-b26434537654\" \"Sub-task\" Todo Medium")
            (println "\nStatus options: Todo, Doing, Done, Later, Now, Waiting, Canceled")
            (println "Priority options: High, Medium, Low")
            (println "\nNote: Creates task WITHOUT #Task visible in title!")
            (js/process.exit 1))]

    (p/let [result (create-task-clean page-or-block-id content status priority)
            task-type (if (uuid? page-or-block-id) "Sub-task" "Top-level task")]
      (if (:error result)
        (do
          (println "Error:" (:error result))
          (js/process.exit 1))
        (do
          (println (str "\n✓ " task-type " created successfully!"))
          (println "  Block ID:" (:id result))
          (println "  UUID:" (:uuid result))
          (println "  Title:" content " (no #Task visible)")
          (println "  Status:" status)
          (println "  Priority:" priority)
          (when (uuid? page-or-block-id)
            (println "  Parent UUID:" page-or-block-id))
          (println "\n✓ Task is immediately visible in Logseq with clean title!"))))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
