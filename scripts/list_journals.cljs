#!/usr/bin/env nbb
(ns list-journals
  "List all journal pages in the database"
  (:require [datascript.core :as d]
            [clojure.string :as string]
            [logseq.db.common.sqlite-cli :as sqlite-cli]
            [nbb.core :as nbb]))

(defn -main [args]
  (let [graph-name (or (first args) "mcp")
        db-path (str (.-HOME js/process.env) "/logseq/graphs/" graph-name "/db.sqlite")
        _ (println "Connecting to graph:" graph-name)

        conn (sqlite-cli/open-db! db-path)

        ;; Find all pages that are journals (have :block/journal-day)
        journals (d/q '[:find (pull ?page [:block/name
                                           :block/title
                                           :block/journal-day
                                           :block/created-at
                                           :block/updated-at])
                        :where
                        [?page :block/journal-day]]
                      @conn)]

    (if (empty? journals)
      (println "No journal pages found in database")
      (do
        (println "\n=== Journal Pages ===" )
        (println "Total journals:" (count journals))
        (println)

        ;; Sort by journal-day descending (most recent first)
        (let [sorted-journals (sort-by #(or (:block/journal-day (first %)) 0)
                                       >
                                       journals)]
          (doseq [[journal] sorted-journals]
            (println "Journal:" (:block/title journal))
            (println "  Name:" (:block/name journal))
            (when-let [day (:block/journal-day journal)]
              (println "  Date:" day))
            (when-let [created (:block/created-at journal)]
              (println "  Created:" (js/Date. created)))
            (println)))))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
