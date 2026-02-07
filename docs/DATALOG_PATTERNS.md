# DataLog Query Patterns for Logseq

## Dynamic Entity Lookup

### Problem
Entity IDs for built-in classes like `Task` can vary between different Logseq graphs. Hardcoding entity IDs (e.g., `151` or `153`) makes queries brittle and graph-specific.

### Solution
Always look up entities dynamically using their `:db/ident` as part of the query.

## Task Queries

### ✓ Correct Pattern - Dynamic Lookup

```clojure
(d/q '[:find (pull ?b [:db/id :block/uuid :block/title
                        {:logseq.property/status [:block/title :db/ident]}
                        {:logseq.property/priority [:block/title]}])
       :where
       ;; Dynamically find Task class entity
       [?task-class :db/ident :logseq.class/Task]
       ;; Find blocks tagged with Task class
       [?b :block/tags ?task-class]
       [?b :logseq.property/status ?status]]
     db)
```

### ✗ Incorrect Pattern - Hardcoded Entity ID

```clojure
(d/q '[:find (pull ?b [:db/id :block/uuid :block/title ...])
       :where
       [?b :block/tags 153]  ; ❌ Hardcoded - will break in other graphs
       [?b :logseq.property/status ?status]]
     db)
```

## Common Built-in Entity Lookups

### Task Class
```clojure
[?task-class :db/ident :logseq.class/Task]
```

### Status Values
```clojure
;; Todo status
[?todo :db/ident :logseq.property/status.todo]

;; Doing status
[?doing :db/ident :logseq.property/status.doing]

;; Done status
[?done :db/ident :logseq.property/status.done]
```

### Priority Values
```clojure
;; High priority
[?high :db/ident :logseq.property/priority.high]

;; Medium priority
[?medium :db/ident :logseq.property/priority.medium]

;; Low priority
[?low :db/ident :logseq.property/priority.low]
```

## Complete Examples

### Find all tasks with a specific status

```clojure
(d/q '[:find (pull ?b [:db/id :block/title])
       :where
       ;; Find Task class
       [?task-class :db/ident :logseq.class/Task]
       ;; Find Done status
       [?done :db/ident :logseq.property/status.done]
       ;; Find tasks with Done status
       [?b :block/tags ?task-class]
       [?b :logseq.property/status ?done]]
     db)
```

### Find high-priority tasks

```clojure
(d/q '[:find (pull ?b [:db/id :block/title
                        {:logseq.property/status [:block/title]}])
       :where
       ;; Find Task class
       [?task-class :db/ident :logseq.class/Task]
       ;; Find High priority
       [?high :db/ident :logseq.property/priority.high]
       ;; Find high-priority tasks
       [?b :block/tags ?task-class]
       [?b :logseq.property/priority ?high]]
     db)
```

### Find tasks on a specific page

```clojure
(d/q '[:find (pull ?b [:db/id :block/title])
       :where
       ;; Find Task class
       [?task-class :db/ident :logseq.class/Task]
       ;; Find page by title
       [?page :block/title "Feb 6th, 2026"]
       ;; Find tasks on that page
       [?b :block/tags ?task-class]
       [?b :block/page ?page]]
     db)
```

## Other Built-in Classes

### Template Class
```clojure
[?template-class :db/ident :logseq.class/Template]
```

### Query Class
```clojure
[?query-class :db/ident :logseq.class/Query]
```

### Card Class
```clojure
[?card-class :db/ident :logseq.class/Card]
```

## Tips

1. **Always use `:db/ident` lookups** for built-in entities
2. **Never hardcode entity IDs** - they vary between graphs
3. **Use descriptive variable names** (e.g., `?task-class` not `?tc`)
4. **Look up once, use many times** - bind entity lookup results to variables
5. **Test queries on multiple graphs** to ensure portability

## Database Paths

All graphs are stored at `~/logseq/graphs/<graph-name>/db.sqlite`.

### Script Pattern

Scripts should accept graph name as the first parameter with a sensible default:

```clojure
(defn -main [args]
  (let [graph-name (or (first args) "mcp")  ; Default to "mcp"
        db-path (str (.-HOME js/process.env) "/logseq/graphs/" graph-name "/db.sqlite")
        _ (println "Connecting to graph:" graph-name)
        conn (sqlite-cli/open-db! db-path)
        db @conn]
    ;; Query logic here
    ))
```

### Usage Examples

```bash
# Use default graph (mcp)
./run-script.sh list_all_tasks.cljs

# Specify a different graph
./run-script.sh list_all_tasks.cljs Demo
./run-script.sh list_all_tasks.cljs my-graph

# With additional parameters
./run-script.sh list_recent_pages.cljs mcp 20
```

### Available Graphs

Check what graphs you have:

```bash
ls ~/logseq/graphs/
```

## Performance Note

Looking up entities by `:db/ident` is efficient - Logseq maintains indexes on these attributes. The overhead is negligible compared to the brittleness of hardcoded IDs.
