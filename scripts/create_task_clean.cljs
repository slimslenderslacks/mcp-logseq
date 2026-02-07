#!/usr/bin/env nbb
(ns create-task-clean
  "Create a task via API with clean title (no visible #Task tag)"
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

(defn create-task-clean [page-name content status priority]
  (p/let [;; Step 1: Create block WITHOUT #Task in content
          block-result (api-call "logseq.Editor.appendBlockInPage"
                                [page-name content])
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
  (let [[page-name content status priority] args
        status (or status "Todo")
        priority (or priority "High")
        _ (when-not (and page-name content)
            (println "Usage: create_task_clean.cljs <page-name> <content> [status] [priority]")
            (println "\nExample:")
            (println "  create_task_clean.cljs \"Feb 5th, 2026\" \"My clean task\" Doing High")
            (println "\nStatus options: Todo, Doing, Done, Later, Now, Waiting, Canceled")
            (println "Priority options: High, Medium, Low")
            (println "\nNote: Creates task WITHOUT #Task visible in title!")
            (js/process.exit 1))]

    (p/let [result (create-task-clean page-name content status priority)]
      (if (:error result)
        (do
          (println "Error:" (:error result))
          (js/process.exit 1))
        (do
          (println "\n✓ Clean task created successfully!")
          (println "  Block ID:" (:id result))
          (println "  UUID:" (:uuid result))
          (println "  Title:" content " (no #Task visible)")
          (println "  Status:" status)
          (println "  Priority:" priority)
          (println "\n✓ Task is immediately visible in Logseq with clean title!"))))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
