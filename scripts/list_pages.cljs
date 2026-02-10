#!/usr/bin/env nbb
(ns list-pages
  "List all pages in a graph"
  (:require [datascript.core :as d]
            [logseq.db.common.sqlite-cli :as sqlite-cli]
            [nbb.core :as nbb]))

(defn uuid-to-string [uuid]
  (if uuid (str uuid) nil))

(defn -main [args]
  (let [graph-name (or (first args) "mcp")
        expand-str (second args)
        expand? (= expand-str "true")
        db-path (str (.-HOME js/process.env) "/logseq/graphs/" graph-name "/db.sqlite")
        _ (println "Connecting to graph:" graph-name)

        conn (sqlite-cli/open-db! db-path)

        ;; Query for all pages with title and uuid
        query (if expand?
                '[:find (pull ?page [:block/title
                                     :block/uuid
                                     :block/created-at
                                     :block/updated-at])
                  :where
                  [?page :block/name]]
                '[:find (pull ?page [:block/title
                                     :block/uuid])
                  :where
                  [?page :block/name]])

        pages (d/q query @conn)

        ;; Sort by title
        sorted-pages (sort-by #(:block/title (first %)) pages)]

    (println "\n=== All Pages ===")
    (println "Total pages:" (count pages))
    (println)

    (doseq [[page] sorted-pages]
      (println "Title:" (:block/title page))
      (println "  UUID:" (uuid-to-string (:block/uuid page)))
      (when expand?
        (when-let [created (:block/created-at page)]
          (println "  Created At:" (js/Date. created)))
        (when-let [updated (:block/updated-at page)]
          (println "  Updated At:" (js/Date. updated))))
      (println))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
