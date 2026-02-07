# Task Completion Time - Quick Reference

## ⏱️ How It Works

```
Task Created          Task Updated          Task Marked DONE
     ↓                     ↓                        ↓
created-at: T1      updated-at: T2          updated-at: T3
                                            done: T3

                    ←──────────────────────────→
                         Duration = T3 - T1
```

## 📊 Properties Tracked

```markdown
- DONE Write documentation
  created-at:: 1704067200000    ← Created: Jan 1, 2024
  updated-at:: 1704153600000    ← Completed: Jan 2, 2024
  done:: 1704153600000          ← Marked DONE: Jan 2, 2024
```

**Duration**: 1704153600000 - 1704067200000 = **86,400,000 ms = 24 hours**

## 🔑 Key Properties

| Property | Type | When Set | Purpose |
|----------|------|----------|---------|
| `created-at` | Integer (ms) | Block creation | Start time |
| `updated-at` | Integer (ms) | Any change | Last modified |
| `done` | Integer (ms) | Marked DONE | Completion marker |
| `todo` | Integer (ms) | Marked TODO | Task marker |
| `doing` | Integer (ms) | Marked DOING | In-progress marker |

## 🔍 Finding DONE Blocks

### Query 1: All DONE tasks
```clojure
{:query (task done)}
```

### Query 2: DONE with timestamps
```clojure
{:query [:find (pull ?b [:block/title
                          :block/created-at
                          :block/updated-at
                          :done])
         :where
         [?b :done ?timestamp]]}
```

## 📐 Calculate Duration

### Simple Formula
```javascript
duration = updated_at - created_at
```

### In Practice
```javascript
// Task timestamps
const created_at = 1704067200000;  // Jan 1, 2024, 00:00:00
const updated_at = 1704153600000;  // Jan 2, 2024, 00:00:00

// Duration calculation
const duration_ms = updated_at - created_at;
const duration_hours = duration_ms / (1000 * 60 * 60);

console.log(`Task took ${duration_hours} hours`);
// Output: Task took 24 hours
```

## 📅 Task Lifecycle Example

```
1. Create task
   - TODO Write docs
     created-at:: 1704067200000
     updated-at:: 1704067200000
     todo:: 1704067200000

2. Start working (4 hours later)
   - DOING Write docs
     created-at:: 1704067200000
     updated-at:: 1704081600000
     doing:: 1704081600000

3. Complete task (20 hours after starting)
   - DONE Write docs
     created-at:: 1704067200000
     updated-at:: 1704153600000
     done:: 1704153600000

   Total duration: 24 hours
   Working duration: 20 hours (from DOING to DONE)
```

## 🎯 Common Calculations

### Total Time (Creation → Completion)
```clojure
(- updated-at created-at)
```

### Active Time (DOING → DONE)
```clojure
(- done doing)
```

### Wait Time (TODO → DOING)
```clojure
(- doing todo)
```

## 🚨 Important Notes

- ❌ **NO `completed-at` field** - Use `updated-at` when task is DONE
- ⚠️ **Editing after DONE changes `updated-at`** - Duration calculation becomes inaccurate
- ✅ **Best practice**: Don't edit DONE tasks or use LOGBOOK for detailed tracking
- 📝 **All timestamps**: Unix epoch milliseconds (e.g., 1704067200000)

## 💡 Tips

### 1. Query for tasks completed today
```clojure
{:query [:find (pull ?b [*])
         :where
         [?b :done ?timestamp]
         [(> ?timestamp START_OF_TODAY)]]}
```

### 2. Format duration for display
```javascript
function formatDuration(ms) {
  const hours = Math.floor(ms / (1000 * 60 * 60));
  const minutes = Math.floor((ms % (1000 * 60 * 60)) / (1000 * 60));
  return `${hours}h ${minutes}m`;
}
```

### 3. Average completion time
```clojure
(defn avg-duration [tasks]
  (let [durations (map #(- (:updated-at %) (:created-at %)) tasks)]
    (/ (reduce + durations) (count durations))))
```

## 🔗 See Also

- Full guide: `TASK_COMPLETION_TRACKING.md`
- Code: `deps/graph-parser/src/logseq/graph_parser/property.cljs:56-81`
- Tests: `deps/graph-parser/test/logseq/graph_parser/exporter_test.cljs:400`

---

**Quick Answer**: Duration = `updated-at` (when DONE) - `created-at`
