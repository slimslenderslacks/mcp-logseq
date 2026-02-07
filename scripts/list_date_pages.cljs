#!/usr/bin/env nbb
(ns list-date-pages
  "List all pages that look like date pages"
  (:require [datascript.core :as d]
            [clojure.string :as string]
            [logseq.db.common.sqlite-cli :as sqlite-cli]
            [nbb.core :as nbb]))

(defn looks-like-date? [page-name]
  "Check if a page name looks like a date (contains only digits and underscores/dashes)"
  (and page-name
       (re-matches #"[\d_\-]+" page-name)))

(defn -main [args]
  (let [graph-name (or (first args) "mcp")
        db-path (str (.-HOME js/process.env) "/logseq/graphs/" graph-name "/db.sqlite")
        _ (println "Connecting to graph:" graph-name)

        conn (sqlite-cli/open-db! db-path)

        ;; Get all pages and check their properties
        all-pages (d/q '[:find (pull ?page [:block/name
                                            :block/title
                                            :block/journal?
                                            :block/journal-day])
                         :where
                         [?page :block/name]]
                       @conn)

        date-like-pages (filter #(looks-like-date? (:block/name (first %))) all-pages)]

    (println "\n=== All Pages Analysis ===")
    (println "Total pages:" (count all-pages))
    (println "Date-like pages:" (count date-like-pages))
    (println "Pages with :block/journal? true:"
             (count (filter #(:block/journal? (first %)) all-pages)))

    (when (seq date-like-pages)
      (println "\n=== Date-like Pages ===")
      (doseq [[page] (take 10 date-like-pages)]
        (println "Name:" (:block/name page))
        (println "  Title:" (:block/title page))
        (println "  Journal?:" (:block/journal? page))
        (println "  Journal-day:" (:block/journal-day page))
        (println)))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
