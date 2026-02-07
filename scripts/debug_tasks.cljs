#!/usr/bin/env nbb
(ns debug-tasks
  "Debug task queries to see what's in the database"
  (:require [datascript.core :as d]
            [logseq.db.common.sqlite-cli :as sqlite-cli]
            [nbb.core :as nbb]))

(defn -main [args]
  (let [graph-name (or (first args) "mcp")
        db-path (str (.-HOME js/process.env) "/logseq/graphs/" graph-name "/db.sqlite")
        _ (println "Connecting to graph:" graph-name)
        conn (sqlite-cli/open-db! db-path)
        db @conn]

    (println "\n=== Finding Task Class Entity ===")
    (let [task-class (d/q '[:find (pull ?e [:db/id :db/ident :block/title])
                            :where
                            [?e :block/title "Task"]]
                          db)]
      (println "Task class:" task-class))

    (println "\n=== Blocks with block/tags ===")
    (let [tagged-blocks (d/q '[:find (pull ?b [:db/id :block/title :block/tags])
                               :where
                               [?b :block/tags ?tag]]
                             db)]
      (println "Count:" (count tagged-blocks))
      (doseq [[block] (take 5 tagged-blocks)]
        (println "Block:" (:db/id block) "-" (:block/title block))
        (println "  Tags:" (:block/tags block))))

    (println "\n=== Blocks with status property ===")
    (let [status-blocks (d/q '[:find (pull ?b [:db/id :block/title
                                                {:logseq.property/status [:block/title :db/ident]}])
                               :where
                               [?b :logseq.property/status ?status]]
                             db)]
      (println "Count:" (count status-blocks))
      (doseq [[block] (take 5 status-blocks)]
        (println "Block:" (:db/id block) "-" (:block/title block))
        (println "  Status:" (get-in block [:logseq.property/status :block/title]))))

    (println "\n=== All blocks (first 10) ===")
    (let [all-blocks (d/q '[:find (pull ?b [:db/id :block/title])
                            :where
                            [?b :block/title ?title]]
                          db)]
      (println "Total blocks:" (count all-blocks))
      (doseq [[block] (take 10 all-blocks)]
        (println "Block:" (:db/id block) "-" (:block/title block))))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
