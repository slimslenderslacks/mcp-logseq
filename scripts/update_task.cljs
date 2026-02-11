#!/usr/bin/env nbb
(ns update-task
  "Update task status and/or content via API"
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

(defn update-task [uuid status content]
  (p/do
    ;; Update status if provided
    (when (and status (not= status ""))
      (p/let [_ (api-call "logseq.Editor.upsertBlockProperty"
                         [uuid "logseq.property/status" status])]
        nil))

    ;; Update content if provided
    (when (and content (not= content ""))
      (p/let [_ (api-call "logseq.Editor.updateBlock"
                         [uuid content])]
        nil))

    ;; Return success
    {:updated true :uuid uuid}))

(defn -main [args]
  (let [[uuid status content] args
        _ (when-not uuid
            (println "Usage: update_task.cljs <block-uuid> <status> <content>")
            (println "\nExamples:")
            (println "  Update status only:")
            (println "    update_task.cljs \"69855f7c-2461-4158-98bd-b26434537654\" Done \"\"")
            (println "  Update content only:")
            (println "    update_task.cljs \"69855f7c-2461-4158-98bd-b26434537654\" \"\" \"New task title\"")
            (println "  Update both:")
            (println "    update_task.cljs \"69855f7c-2461-4158-98bd-b26434537654\" Doing \"Updated task\"")
            (println "\nStatus options: Todo, Doing, Done, Later, Now, Waiting, Canceled")
            (println "\nNote: Use empty string \"\" to skip updating a field")
            (js/process.exit 1))]

    (p/let [result (update-task uuid status content)
            updates (cond-> []
                      (and status (not= status "")) (conj (str "Status: " status))
                      (and content (not= content "")) (conj (str "Content: " content)))]
      (println "\n✓ Task update sent")
      (println "  UUID:" uuid)
      (when (seq updates)
        (println "  Updated:")
        (doseq [update updates]
          (println "   -" update)))
      (println "\nCheck Logseq to verify the changes took effect."))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
