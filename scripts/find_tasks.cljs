#!/usr/bin/env nbb
(ns find-tasks
  "Find all tasks and their markers in a graph"
  (:require ["fs" :as fs]
            [clojure.string :as string]
            [datascript.core :as d]
            [logseq.db.common.sqlite-cli :as sqlite-cli]
            [nbb.core :as nbb]))

(defn -main [args]
  (let [graph-name (or (first args) "mcp")
        db-path (str (.-HOME js/process.env) "/logseq/graphs/" graph-name "/db.sqlite")
        _ (println "Connecting to graph:" graph-name)
        _ (when-not (fs/existsSync db-path)
            (println "Error: Database does not exist:" db-path)
            (js/process.exit 1))

        conn (sqlite-cli/open-db! db-path)

        ;; Search for blocks with any task marker property
        markers [:todo :doing :now :later :done :canceled :cancelled :waiting :wait]

        _ (println "\n=== Searching for task markers ===\n")

        results (for [marker markers
                      :let [found (d/q [:find '(pull ?b [:block/title :block/created-at])
                                        :in '$ '?marker
                                        :where ['?b '?marker '_]]
                                       @conn marker)]
                      :when (seq found)]
                  [marker (count found)])

        ;; Also search for blocks that might have task-like content
        task-content (d/q '[:find (pull ?b [:block/title])
                            :where
                            [?b :block/title ?title]
                            [(re-find #"(?i)(TODO|DONE|DOING|LATER|NOW|WAITING)" ?title)]]
                          @conn)]

    (if (seq results)
      (do
        (println "Found task markers:")
        (doseq [[marker count] results]
          (println (str "  " marker ": " count " tasks")))
        (println))
      (println "No task marker properties found.\n"))

    (when (seq task-content)
      (println (str "Found " (count task-content) " blocks with task keywords in title:"))
      (doseq [[block] (take 10 task-content)]
        (println "  -" (:block/title block))))

    (when-not (or (seq results) (seq task-content))
      (println "No tasks found in this graph."))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
