#!/usr/bin/env nbb
(ns query-status-tasks
  (:require [datascript.core :as d]
            [logseq.db.common.sqlite-cli :as sqlite-cli]
            [nbb.core :as nbb]))

(defn format-timestamp [ts]
  (when ts
    (let [date (js/Date. ts)]
      (.toLocaleDateString date "en-US" #js {:year "numeric" :month "short" :day "numeric"}))))

(defn -main [args]
  (let [graph-name (or (first args) "mcp")
        db-path (str (.-HOME js/process.env) "/logseq/graphs/" graph-name "/db.sqlite")
        _ (println "Connecting to graph:" graph-name)
        conn (sqlite-cli/open-db! db-path)
        db @conn
        
        ;; First, find all status values
        all-statuses (d/q '[:find ?status-name
                            :where
                            [?status :block/name ?status-name]
                            [?status :block/tags ?tag]
                            [?tag :db/ident :logseq.class/Property]]
                          db)
        
        ;; Query for tasks with status property
        tasks-with-status (d/q '[:find (pull ?b [:block/title :block/created-at :block/updated-at
                                                  {:logseq.property/status [:block/title :block/name]}
                                                  {:block/page [:block/title]}])
                                 :where
                                 [?b :logseq.property/status ?status]]
                               db)]
    
    (println "\n=== Available Status Values ===")
    (doseq [[status-name] (sort all-statuses)]
      (println " -" status-name))
    
    (println "\n=== Tasks by Status ===")
    (println "Found" (count tasks-with-status) "tasks with status\n")
    
    ;; Group by status
    (let [grouped (group-by #(get-in (first %) [:logseq.property/status :block/name]) tasks-with-status)]
      (doseq [[status tasks] (sort-by first grouped)]
        (println "\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        (println "STATUS:" (or status "unknown") "-" (count tasks) "tasks")
        (println "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        (doseq [[task] (take 10 tasks)]
          (println "\n✓" (:block/title task))
          (when-let [page-title (get-in task [:block/page :block/title])]
            (println "  Page:" page-title))
          (when-let [updated (:block/updated-at task)]
            (println "  Updated:" (format-timestamp updated))))))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
