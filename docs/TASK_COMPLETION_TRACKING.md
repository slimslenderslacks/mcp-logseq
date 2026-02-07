# Task Completion Time Tracking in Logseq

## Overview

Logseq tracks task completion time using **timestamps** and **task state markers**. This document explains how it works and how to calculate task duration.

## 🕐 Timestamps

Every block in Logseq can have two timestamps:

| Property | Type | Description |
|----------|------|-------------|
| `created-at` | Integer (milliseconds) | When the block was created |
| `updated-at` | Integer (milliseconds) | When the block was last modified |

### Code Reference

From `block.cljs:709-715`:
```clojure
{:keys [created-at updated-at]} (:properties properties)
block (cond-> block
        (and created-at (integer? created-at))
        (assoc :block/created-at created-at)

        (and updated-at (integer? updated-at))
        (assoc :block/updated-at updated-at))
```

## 🔄 Task State Markers

Defined in `property.cljs:56-81`, tasks can have these states:

```clojure
; Task markers (line 57)
:todo :doing :now :later :done

; Each stored as integer timestamp (lines 77-81)
:todo :integer
:doing :integer
:now :integer
:later :integer
:done :integer
```

### What This Means

- Each state marker stores a **timestamp** (in milliseconds)
- When you mark a task as `DONE`, the `:done` property gets the current timestamp
- The `updated-at` timestamp is also updated

## ⏱️ Calculating Completion Time

### Formula

```
Duration = updated-at (when DONE) - created-at
```

### Important Note

**There is NO dedicated `completed-at` field**. Logseq infers completion time from:
- The `:done` marker timestamp
- The `updated-at` timestamp when the task was marked done

## 📝 Example: Task Lifecycle

### Markdown Example

```markdown
- TODO Write documentation
  created-at:: 1704067200000
  updated-at:: 1704067200000
```

After marking as DONE:

```markdown
- DONE Write documentation
  created-at:: 1704067200000
  updated-at:: 1704153600000
  done:: 1704153600000
```

### Timestamps Explained

```javascript
// Created: Jan 1, 2024, 00:00:00 GMT
created-at: 1704067200000

// Marked done: Jan 2, 2024, 00:00:00 GMT
updated-at: 1704153600000
done: 1704153600000

// Duration = 1704153600000 - 1704067200000 = 86400000 ms = 24 hours
```

## 🔍 Querying DONE Tasks

### Basic Query

From `exporter_test.cljs:400`:

```clojure
{:query (task todo doing)}
```

This finds all tasks with TODO or DOING state.

### Query for DONE Tasks

```clojure
{:query (task done)}
```

### Query with Properties

```clojure
{:query (and
          (task done)
          (property :created-at))
 :keys [:block/title :block/created-at :block/updated-at]}
```

## 📊 Calculating Duration in Queries

### JavaScript/ClojureScript Example

```clojure
(defn task-duration [block]
  (let [created (:block/created-at block)
        updated (:block/updated-at block)]
    (when (and created updated)
      (- updated created))))

; Usage
(let [done-tasks (q '[:find (pull ?b [*])
                      :where
                      [?b :done ?done-timestamp]])]
  (map (fn [[block]]
         (assoc block :duration (task-duration block)))
       done-tasks))
```

### Duration Formatting

```clojure
(defn format-duration [ms]
  (let [seconds (/ ms 1000)
        minutes (/ seconds 60)
        hours (/ minutes 60)
        days (/ hours 24)]
    (cond
      (>= days 1) (str (Math/round days) " days")
      (>= hours 1) (str (Math/round hours) " hours")
      (>= minutes 1) (str (Math/round minutes) " minutes")
      :else (str (Math/round seconds) " seconds"))))
```

## 🎯 Practical Examples

### Example 1: Find All DONE Tasks with Duration

```clojure
{:title "Completed Tasks"
 :query [:find (pull ?b [:block/title
                          :block/created-at
                          :block/updated-at
                          :done])
         :where
         [?b :done ?timestamp]
         [?b :block/created-at ?created]
         [?b :block/updated-at ?updated]]}
```

### Example 2: Tasks Completed Today

```clojure
{:title "Completed Today"
 :query [:find (pull ?b [:block/title :done])
         :where
         [?b :done ?timestamp]
         [(> ?timestamp TODAY_START_MS)]]}
```

### Example 3: Average Completion Time

```clojure
(defn average-task-duration [done-tasks]
  (let [durations (keep (fn [task]
                          (when-let [duration (task-duration task)]
                            duration))
                        done-tasks)
        total (reduce + durations)
        count (count durations)]
    (when (pos? count)
      (/ total count))))
```

## 📅 Scheduled & Deadline Interaction

Logseq also tracks:
- `:scheduled` - When task should start
- `:deadline` - When task must be completed

From `block.cljs:270-286`:

```clojure
(defn timestamps->scheduled-and-deadline [timestamps]
  (let [timestamps (update-keys timestamps (comp keyword string/lower-case))
        m (some->> (select-keys timestamps [:scheduled :deadline])
                   (map (fn [[k v]]
                          (let [{:keys [date repetition]} v
                                {:keys [year month day]} date]
                            ...))))]
    m))
```

These are **separate** from completion tracking but can be used to calculate:
- **Early/Late completion**: `done_timestamp - deadline`
- **Start delay**: `updated_at - scheduled`

## 🔧 Implementation in Graph Parser

### File Graphs vs DB Graphs

**File Graphs** (legacy):
- Parse `TODO`, `DONE` markers from text
- Store as properties

**DB Graphs** (modern):
- Markers disabled during parsing (`mldoc.cljc:147`)
- Use typed properties instead
- More efficient for large graphs

### Marker Recognition

From `exporter.cljs:717`:

```clojure
:now :later :doing :done :canceled :cancelled
:in-progress :todo :wait :waiting
```

Logseq recognizes various synonyms for task states.

## 📖 Database Schema

From `property.cljs`, the relevant properties:

```clojure
; Timestamps (integers in milliseconds)
:created-at :integer
:updated-at :integer

; Task states (integers - timestamp when entered that state)
:todo :integer
:doing :integer
:now :integer
:later :integer
:done :integer

; Dates
:scheduled :integer  ; Day number
:deadline :integer   ; Day number
```

## 💡 Best Practices

### 1. Automatic Timestamps

Let Logseq automatically set `created-at` and `updated-at`:
```markdown
- TODO My task
  created-at:: [[timestamp]]
  updated-at:: [[timestamp]]
```

### 2. Manual Time Tracking

Use LOGBOOK entries for detailed tracking:
```org-mode
- DONE Task name
  :LOGBOOK:
  CLOCK: [2024-01-01 Mon 09:00]--[2024-01-01 Mon 11:00] =>  2:00
  :END:
```

From `property.cljs:180-202`, Logseq can parse LOGBOOK drawers.

### 3. Query Reports

Create queries to generate duration reports:
```clojure
{:title "Task Duration Report"
 :query [:find ?title ?duration
         :where
         [?b :block/title ?title]
         [?b :done _]
         [?b :block/created-at ?created]
         [?b :block/updated-at ?updated]
         [(- ?updated ?created) ?duration]]}
```

## 🎨 Visualization Example

```javascript
// Convert to human-readable format
const tasks = [
  {title: "Write docs", created: 1704067200000, updated: 1704153600000},
  {title: "Code review", created: 1704070800000, updated: 1704085200000}
];

tasks.forEach(task => {
  const duration = task.updated - task.created;
  const hours = Math.floor(duration / (1000 * 60 * 60));
  const minutes = Math.floor((duration % (1000 * 60 * 60)) / (1000 * 60));
  console.log(`${task.title}: ${hours}h ${minutes}m`);
});

// Output:
// Write docs: 24h 0m
// Code review: 4h 0m
```

## 🚨 Limitations

1. **No built-in `completed-at`** - Must use `updated-at` as proxy
2. **Single timestamp on state change** - If you mark as DONE, then edit the block, `updated-at` changes
3. **Manual tracking required** - For paused/resumed tasks, use LOGBOOK entries
4. **Query complexity** - Duration calculations must be done client-side or in advanced queries

## 🔗 Code References

| File | Lines | Purpose |
|------|-------|---------|
| `property.cljs` | 56-81 | Task marker definitions |
| `block.cljs` | 709-715 | Timestamp assignment |
| `exporter.cljs` | 717 | Marker recognition |
| `exporter_test.cljs` | 400 | Query example |
| `mldoc.cljc` | 147 | DB graph marker parsing |

## ✅ Summary

**How Logseq tracks completion time:**

1. **Created**: Block gets `created-at` timestamp
2. **Updated**: Any change updates `updated-at`
3. **Marked DONE**: Sets `:done` property and updates `updated-at`
4. **Duration**: `updated-at - created-at` when task is DONE

**Key Points:**
- ✅ All timestamps are integers (milliseconds since Unix epoch)
- ✅ Task states (`:todo`, `:done`, etc.) are stored as integer properties
- ✅ No dedicated `completed-at` field - inferred from `updated-at`
- ✅ Queries can filter and calculate durations
- ✅ LOGBOOK entries available for detailed time tracking
- ✅ Scheduled/deadline dates are separate from completion tracking

---

**Need more advanced time tracking?** Consider:
- Using LOGBOOK entries with CLOCK timestamps
- Custom queries to calculate duration
- External tools that analyze Logseq databases
- Plugins that add time tracking features
