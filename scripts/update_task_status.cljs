#!/usr/bin/env nbb
(ns update-task-status
  "Update task status via API without breaking structure"
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

(defn update-task-status [uuid status]
  (p/let [;; ONLY update the status property
          result (api-call "logseq.Editor.upsertBlockProperty"
                          [uuid "logseq.property/status" status])]
    result))

(defn -main [args]
  (let [[uuid status] args
        _ (when-not (and uuid status)
            (println "Usage: update_task_status.cljs <block-uuid> <status>")
            (println "\nExample:")
            (println "  update_task_status.cljs \"69855f7c-2461-4158-98bd-b26434537654\" Done")
            (println "\nStatus options: Todo, Doing, Done, Later, Now, Waiting, Canceled, In Review")
            (js/process.exit 1))]

    (p/let [result (update-task-status uuid status)]
      (println "\n✓ Status update sent")
      (println "  UUID:" uuid)
      (println "  New status:" status)
      (println "\nCheck Logseq to verify the change took effect."))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
