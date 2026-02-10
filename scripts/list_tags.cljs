#!/usr/bin/env nbb
(ns list-tags
  "List all tags in a graph"
  (:require [datascript.core :as d]
            [logseq.db.common.sqlite-cli :as sqlite-cli]
            [logseq.db.frontend.property :as db-property]
            [nbb.core :as nbb]))

(defn uuid-to-string [uuid]
  (if uuid (str uuid) nil))

(defn list-tags
  "Main fn for ListTags tool"
  [db {:keys [expand]}]
  (->> (d/datoms db :avet :block/tags :logseq.class/Tag)
       (map #(d/entity db (:e %)))
       (map (fn [e]
              (if expand
                (cond-> (into {} e)
                  true
                  (dissoc e :block/tags :block/order :block/refs :block/name
                          :logseq.property.embedding/hnsw-label-updated-at)
                  true
                  (update :block/uuid str)
                  (:logseq.property.class/extends e)
                  (update :logseq.property.class/extends #(mapv :db/ident %))
                  (:logseq.property.class/properties e)
                  (update :logseq.property.class/properties #(mapv :db/ident %))
                  (:logseq.property.view/type e)
                  (assoc :logseq.property.view/type (:db/ident (:logseq.property.view/type e)))
                  (:logseq.property/description e)
                  (update :logseq.property/description db-property/property-value-content))
                {:block/title (:block/title e)
                 :block/uuid (str (:block/uuid e))})))))

(defn -main [args]
  (let [graph-name (or (first args) "mcp")
        expand-str (second args)
        expand? (= expand-str "true")
        db-path (str (.-HOME js/process.env) "/logseq/graphs/" graph-name "/db.sqlite")
        _ (println "Connecting to graph:" graph-name)

        conn (sqlite-cli/open-db! db-path)
        db @conn

        tags (list-tags db {:expand expand?})]

    (println "\n=== All Tags ===")
    (println "Total tags:" (count tags))
    (println)

    (doseq [tag tags]
      (println "Title:" (:block/title tag))
      (println "  UUID:" (:block/uuid tag))
      (when expand?
        (when-let [extends (:logseq.property.class/extends tag)]
          (println "  Extends:" (pr-str extends)))
        (when-let [properties (:logseq.property.class/properties tag)]
          (println "  Tag Properties:" (pr-str properties)))
        (when-let [view-type (:logseq.property.view/type tag)]
          (println "  View Type:" view-type))
        (when-let [description (:logseq.property/description tag)]
          (println "  Description:" description))
        (when-let [icon (:logseq.property/icon tag)]
          (println "  Icon:" (pr-str icon)))
        (when-let [created-at (:block/created-at tag)]
          (println "  Created At:" created-at))
        (when-let [updated-at (:block/updated-at tag)]
          (println "  Updated At:" updated-at)))
      (println))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
