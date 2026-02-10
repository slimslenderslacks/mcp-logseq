#!/usr/bin/env nbb
(ns list-properties
  "List all properties in a graph"
  (:require [datascript.core :as d]
            [logseq.db.common.sqlite-cli :as sqlite-cli]
            [logseq.db.frontend.property :as db-property]
            [nbb.core :as nbb]))

(defn uuid-to-string [uuid]
  (if uuid (str uuid) nil))

(defn list-properties
  "Main fn for ListProperties tool"
  [db {:keys [expand]}]
  (->> (d/datoms db :avet :block/tags :logseq.class/Property)
       (map #(d/entity db (:e %)))
       (map (fn [e]
              (if expand
                (cond-> (into {} e)
                  true
                  (dissoc e :block/tags :block/order :block/refs :block/name :db/index
                          :logseq.property.embedding/hnsw-label-updated-at :logseq.property/default-value)
                  true
                  (update :block/uuid str)
                  (:logseq.property/classes e)
                  (update :logseq.property/classes #(mapv :db/ident %))
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

        properties (list-properties db {:expand expand?})]

    (println "\n=== All Properties ===")
    (println "Total properties:" (count properties))
    (println)

    (doseq [prop properties]
      (println "Title:" (:block/title prop))
      (println "  UUID:" (:block/uuid prop))
      (when expand?
        (when-let [prop-type (:logseq.property/type prop)]
          (println "  Property Type:" (:db/ident prop-type)))
        (when-let [classes (:logseq.property/classes prop)]
          (println "  Classes:" (pr-str classes)))
        (when-let [schema (:logseq.property/schema prop)]
          (println "  Schema:" (pr-str schema)))
        (when-let [cardinality (:logseq.property/cardinality prop)]
          (println "  Cardinality:" cardinality))
        (when-let [description (:logseq.property/description prop)]
          (println "  Description:" description))
        (when-let [public? (:logseq.property/public? prop)]
          (println "  Public?:" public?))
        (when-let [closed-values (:logseq.property/closed-values prop)]
          (println "  Closed Values:" (pr-str (mapv :block/title closed-values))))
        (when-let [created-at (:block/created-at prop)]
          (println "  Created At:" created-at))
        (when-let [updated-at (:block/updated-at prop)]
          (println "  Updated At:" updated-at)))
      (println))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
