#!/usr/bin/env nbb
(ns find-block-by-title
  "Find a block by title and show all its properties"
  (:require [datascript.core :as d]
            [clojure.string :as string]
            [logseq.db.common.sqlite-cli :as sqlite-cli]
            [nbb.core :as nbb]))

(defn -main [args]
  (let [[graph-name page-name title] args
        _ (when-not (and graph-name page-name title)
            (println "Usage: find_block_by_title.cljs <graph-name> <page-name> <title>")
            (println "\nExample: find_block_by_title.cljs mcp \"Feb 6th, 2026\" \"task title\"")
            (js/process.exit 1))

        db-path (str (.-HOME js/process.env) "/logseq/graphs/" graph-name "/db.sqlite")
        _ (println "Connecting to graph:" graph-name)
        conn (sqlite-cli/open-db! db-path)
        normalized-name (string/lower-case page-name)

        ;; Find blocks with this title on the page
        blocks (d/q '[:find (pull ?b [*])
                      :in $ ?page-name ?title
                      :where
                      [?page :block/name ?page-name]
                      [?b :block/page ?page]
                      [?b :block/title ?title]]
                    @conn normalized-name title)]

    (if (empty? blocks)
      (println "No block found with title:" title)
      (doseq [[block] blocks]
        (println "\n=== Block Found ===")
        (println "Block ID:" (:db/id block))
        (println "UUID:" (:block/uuid block))
        (println "\nAll properties:")
        (doseq [[k v] (sort-by first block)]
          (println (str "  " k ":") v))))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
