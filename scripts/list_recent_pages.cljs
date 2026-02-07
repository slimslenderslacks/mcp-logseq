#!/usr/bin/env nbb
(ns list-recent-pages
  "List most recently created pages"
  (:require [datascript.core :as d]
            [logseq.db.common.sqlite-cli :as sqlite-cli]
            [nbb.core :as nbb]))

(defn -main [args]
  (let [graph-name (or (first args) "mcp")
        limit-str (second args)
        limit (if limit-str (js/parseInt limit-str) 10)
        db-path (str (.-HOME js/process.env) "/logseq/graphs/" graph-name "/db.sqlite")
        _ (println "Connecting to graph:" graph-name)

        conn (sqlite-cli/open-db! db-path)

        ;; Get all pages with creation date
        pages (d/q '[:find (pull ?page [:block/name
                                        :block/title
                                        :block/journal?
                                        :block/journal-day
                                        :block/created-at
                                        :block/updated-at])
                     :where
                     [?page :block/name]]
                   @conn)

        ;; Sort by created-at descending
        sorted-pages (sort-by #(or (:block/created-at (first %)) 0) > pages)]

    (println "\n=== Recently Created Pages ===")
    (println "Total pages:" (count pages))
    (println "Showing" limit "most recent:")
    (println)

    (doseq [[page] (take limit sorted-pages)]
      (println (:block/title page))
      (println "  Name:" (:block/name page))
      (when-let [created (:block/created-at page)]
        (println "  Created:" (js/Date. created)))
      (when-let [updated (:block/updated-at page)]
        (println "  Updated:" (js/Date. updated)))
      (when (:block/journal? page)
        (println "  Journal?: true"))
      (when-let [day (:block/journal-day page)]
        (println "  Journal day:" day))
      (println))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
