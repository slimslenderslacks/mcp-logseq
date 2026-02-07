#!/usr/bin/env nbb
(ns list-tasks-by-status
  "List tasks grouped by status"
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
                   db)

        grouped (group-by #(get-in (first %) [:logseq.property/status :block/title]) tasks)]

    (println "\n=== Task Summary ===")
    (println "Total tasks:" (count tasks))
    (println)

    (doseq [[status task-list] (sort-by first grouped)]
      (println (str status ": " (count task-list)))
      (doseq [[task] (sort-by #(get-in (first %) [:logseq.property/priority :block/title]) > task-list)]
        (let [priority (get-in task [:logseq.property/priority :block/title])
              title (:block/title task)]
          (if priority
            (println "  [" priority "]" title)
            (println "  " title)))))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
