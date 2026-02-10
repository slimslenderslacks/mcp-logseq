#!/usr/bin/env nbb
(ns get-page
  "Get a page's content including its blocks. A property and a tag are pages."
  (:require [datascript.core :as d]
            [logseq.db.common.sqlite-cli :as sqlite-cli]
            [nbb.core :as nbb]))

(defn uuid-to-string [uuid]
  (if uuid (str uuid) nil))

(defn find-page-by-name-or-uuid [db page-identifier]
  ;; Try to find by name first
  (let [by-name (d/q '[:find (pull ?page [:db/id
                                           :block/name
                                           :block/title
                                           :block/uuid
                                           :block/journal?
                                           :block/journal-day
                                           :block/created-at
                                           :block/updated-at])
                       :in $ ?name
                       :where
                       [?page :block/name ?name]]
                     db
                     (clojure.string/lower-case page-identifier))]
    (if (seq by-name)
      (first (first by-name))
      ;; Try to find by UUID
      (let [by-uuid (d/q '[:find (pull ?page [:db/id
                                               :block/name
                                               :block/title
                                               :block/uuid
                                               :block/journal?
                                               :block/journal-day
                                               :block/created-at
                                               :block/updated-at])
                           :in $ ?uuid-str
                           :where
                           [?page :block/uuid ?uuid]
                           [(str ?uuid) ?uuid-str]]
                         db
                         page-identifier)]
        (when (seq by-uuid)
          (first (first by-uuid)))))))

(defn get-page-blocks [db page-id]
  (let [blocks (d/q '[:find (pull ?block [:db/id
                                          :block/uuid
                                          :block/title
                                          :block/content
                                          :block/order
                                          :block/properties])
                      :in $ ?page-id
                      :where
                      [?block :block/page ?page-id]
                      [?block :block/title]]
                    db
                    page-id)]
    (sort-by #(:block/order (first %) 0) blocks)))

(defn -main [args]
  (let [graph-name (or (first args) "mcp")
        page-name (second args)
        db-path (str (.-HOME js/process.env) "/logseq/graphs/" graph-name "/db.sqlite")
        _ (println "Connecting to graph:" graph-name)]

    (when-not page-name
      (println "Error: page name or UUID is required")
      (js/process.exit 1))

    (let [conn (sqlite-cli/open-db! db-path)
          db @conn
          page (find-page-by-name-or-uuid db page-name)]

      (if-not page
        (do
          (println "Error: Page not found:" page-name)
          (js/process.exit 1))
        (do
          (println "\n=== Page Information ===")
          (println "Title:" (:block/title page))
          (println "Name:" (:block/name page))
          (println "UUID:" (uuid-to-string (:block/uuid page)))
          (when-let [created (:block/created-at page)]
            (println "Created At:" (js/Date. created)))
          (when-let [updated (:block/updated-at page)]
            (println "Updated At:" (js/Date. updated)))
          (when (:block/journal? page)
            (println "Journal?: true")
            (when-let [day (:block/journal-day page)]
              (println "Journal Day:" day)))
          (println)

          ;; Get and display blocks
          (let [blocks (get-page-blocks db (:db/id page))]
            (println "=== Page Blocks ===")
            (println "Total blocks:" (count blocks))
            (println)

            (doseq [[block] blocks]
              (println "Block UUID:" (uuid-to-string (:block/uuid block)))
              (when-let [title (:block/title block)]
                (println "  Title:" title))
              (when-let [content (:block/content block)]
                (println "  Content:" content))
              (when-let [properties (:block/properties block)]
                (println "  Properties:" properties))
              (when-let [order (:block/order block)]
                (println "  Order:" order))
              (println))))))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
