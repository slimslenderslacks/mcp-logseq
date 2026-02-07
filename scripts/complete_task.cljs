#!/usr/bin/env nbb
(ns complete-task
  "Mark a task as complete (Done status) via API"
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

(defn complete-task [uuid-or-content]
  (p/let [;; Update status to Done
          result (api-call "logseq.Editor.upsertBlockProperty"
                          [uuid-or-content "logseq.property/status" "Done"])]
    result))

(defn -main [args]
  (let [[uuid] args
        _ (when-not uuid
            (println "Usage: complete_task.cljs <block-uuid>")
            (println "\nExample:")
            (println "  complete_task.cljs \"69855f7c-2461-4158-98bd-b26434537654\"")
            (println "\nMarks a task as Done (completed).")
            (js/process.exit 1))]

    (p/let [result (complete-task uuid)]
      (if (:error result)
        (do
          (println "Error:" (:error result))
          (js/process.exit 1))
        (do
          (println "\n✓ Task marked as complete!")
          (println "  UUID:" uuid)
          (println "  Status: Done")
          (println "\nTask is now marked as completed in Logseq!"))))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
