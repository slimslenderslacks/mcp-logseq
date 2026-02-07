#!/usr/bin/env nbb
(ns list-done-tasks-v2
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
        
        ;; Query for tasks with status = Done
        done-tasks (d/q '[:find (pull ?b [:block/title :block/created-at :block/updated-at
                                          {:logseq.property/status [:block/title :db/ident]}
                                          {:block/page [:block/title]}])
                          :where
                          [?b :logseq.property/status ?status]
                          [?status :db/ident :logseq.property/status.done]]
                        db)]
    
    (println "\n=== DONE Tasks ===")
    (println "Found" (count done-tasks) "DONE tasks\n")
    
    (doseq [[task] (sort-by #(or (:block/updated-at %) 0) > done-tasks)]
      (let [title (:block/title task)
            created (:block/created-at task)
            updated (:block/updated-at task)
            page-title (get-in task [:block/page :block/title])
            duration (when (and created updated)
                      (- updated created))
            duration-hours (when duration
                            (/ duration 1000 60 60))
            duration-days (when duration
                           (/ duration 1000 60 60 24))]
        (println "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        (println "✓" title)
        (when page-title
          (println "  Page:" page-title))
        (when created
          (println "  Created:" (format-timestamp created)))
        (when updated
          (println "  Completed:" (format-timestamp updated)))
        (when (and duration-hours (> duration-hours 0))
          (if (>= duration-days 1)
            (println "  Duration:" (.toFixed duration-days 1) "days")
            (println "  Duration:" (.toFixed duration-hours 1) "hours")))
        (println)))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
