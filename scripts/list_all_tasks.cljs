#!/usr/bin/env nbb
(ns list-all-tasks
  "List all tasks with their current status"
  (:require [datascript.core :as d]
            [logseq.db.common.sqlite-cli :as sqlite-cli]
            [nbb.core :as nbb]))

(defn -main [args]
  (let [graph-name (or (first args) "mcp")
        db-path (str (.-HOME js/process.env) "/logseq/graphs/" graph-name "/db.sqlite")
        _ (println "Connecting to graph:" graph-name)
        conn (sqlite-cli/open-db! db-path)
        db @conn

        tasks (d/q '[:find (pull ?b [:db/id :block/uuid :block/title
                                      {:logseq.property/status [:block/title :db/ident]}
                                      {:logseq.property/priority [:block/title]}])
                     :where
                     ;; Dynamically find Task class entity
                     [?task-class :db/ident :logseq.class/Task]
                     ;; Find blocks tagged with Task class
                     [?b :block/tags ?task-class]
                     [?b :logseq.property/status ?status]]
                   db)]

    (println "\n=== All Tasks ===\n")
    (doseq [task (sort-by #(:db/id (first %)) tasks)]
      (let [t (first task)]
        (println "Task ID:" (:db/id t))
        (println "  UUID:" (:block/uuid t))
        (println "  Title:" (:block/title t))
        (println "  Status:" (get-in t [:logseq.property/status :block/title]))
        (println "  Priority:" (get-in t [:logseq.property/priority :block/title]))
        (println)))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
