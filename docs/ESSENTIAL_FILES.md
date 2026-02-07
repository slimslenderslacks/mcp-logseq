# Essential Files Reference

## Quick Summary

After cleanup, you'll have **20 essential files** for working with Logseq's SQLite database and HTTP API.

---

## API Scripts (6 files)

Located in `script/` directory.

### Task Management

**`complete_task.cljs`**
- **Purpose:** Mark a task as Done
- **Usage:** `./node_modules/.bin/nbb-logseq -cp script:src script/complete_task.cljs <uuid>`
- **Example:** `./node_modules/.bin/nbb-logseq -cp script:src script/complete_task.cljs "6985659e-59b1-47e7-8661-ee790462f263"`

**`update_task_status.cljs`**
- **Purpose:** Update task status to any value
- **Usage:** `./node_modules/.bin/nbb-logseq -cp script:src script/update_task_status.cljs <uuid> <status>`
- **Statuses:** Todo, Doing, Done, Later, Now, Waiting, Canceled, In Review
- **Example:** `./node_modules/.bin/nbb-logseq -cp script:src script/update_task_status.cljs "6985659e-59b1-47e7-8661-ee790462f263" "Doing"`

**`create_task_clean.cljs`** ⭐ Primary
- **Purpose:** Create tasks with clean titles (no visible #Task tag)
- **Usage:** `./node_modules/.bin/nbb-logseq -cp script:src script/create_task_clean.cljs <page> <content> [status] [priority]`
- **Example:** `./node_modules/.bin/nbb-logseq -cp script:src script/create_task_clean.cljs "Feb 5th, 2026" "Implement feature" "Todo" "High"`

### Task Information

**`get_task_info.cljs`**
- **Purpose:** Get detailed task information
- **Usage:** `./node_modules/.bin/nbb-logseq -cp script:src script/get_task_info.cljs <uuid>`
- **Shows:** Content, status, priority, full block data

**`list_page_blocks.cljs`**
- **Purpose:** List all blocks on a page
- **Usage:** `./node_modules/.bin/nbb-logseq -cp script:src script/list_page_blocks.cljs [page-name]`
- **Default page:** "Feb 5th, 2026"

**`list_page_tasks_api.cljs`**
- **Purpose:** List tasks on a page with their properties
- **Usage:** `./node_modules/.bin/nbb-logseq -cp script:src script/list_page_tasks_api.cljs [page-name]`

---

## Database Query Scripts (8 files)

Located in `script/` directory. These query the SQLite database directly using DataScript/DataLog.

### Task Queries

**`list_done_tasks_v2.cljs`** ⭐ Primary
- **Purpose:** Query all DONE tasks with timestamps and duration
- **Database:** `~/.logseq/graphs/logseq_db_mcp`
- **Shows:** Task title, page, created/completed dates, duration

**`list_all_tasks.cljs`**
- **Purpose:** Query all tasks regardless of status
- **Database:** Demo database
- **Shows:** UUID, title, status, priority

**`query_status_tasks.cljs`**
- **Purpose:** Query tasks by specific status
- **Usage:** Can filter by Todo, Doing, Done, etc.

**`find_tasks.cljs`**
- **Purpose:** Find tasks by various criteria
- **Flexible:** Can search by title, status, priority, etc.

### Block & Page Queries

**`find_block_by_title.cljs`**
- **Purpose:** Find blocks matching a title pattern
- **Useful for:** Locating specific content

**`list_journals.cljs`**
- **Purpose:** List all journal pages
- **Shows:** Journal pages with their dates

**`list_date_pages.cljs`**
- **Purpose:** List pages organized by date
- **Shows:** Date-based page structure

**`list_recent_pages.cljs`**
- **Purpose:** List recently modified pages
- **Shows:** Pages sorted by update time

---

## Documentation (6 files)

### Primary Documentation

**`SUMMARY.md`** ⭐ Comprehensive Reference
- Complete exploration summary
- Use cases and scripts
- HTTP API reference
- Database querying guide
- Data model reference
- Testing results

**`FILE_CLEANUP_PLAN.md`** ⭐ Cleanup Guide
- Files to remove and why
- Files to keep and why
- Cleanup commands

**`ESSENTIAL_FILES.md`** (this file)
- Quick reference for essential files
- Usage examples

### Additional Documentation

**`QUICKSTART.md`**
- Getting started guide
- Basic usage examples

**`IMPLEMENTATION_SUMMARY.md`**
- Implementation details
- Technical decisions

**`SOLUTION_SUMMARY.md`**
- Solution overview
- Architecture summary

**`TASK_COMPLETION_TRACKING.md`**
- Task tracking guide
- Workflow documentation

**`TASK_TRACKING_QUICK_REFERENCE.md`**
- Quick reference for task operations
- Command cheat sheet

---

## Workflow Examples

### Creating and Managing a Task

```bash
# 1. Create a high-priority task
./node_modules/.bin/nbb-logseq -cp script:src \
  script/create_task_clean.cljs \
  "Feb 5th, 2026" \
  "Implement authentication" \
  "Todo" \
  "High"
# Output: UUID: 6985659e-59b1-47e7-8661-ee790462f263

# 2. Get task details
./node_modules/.bin/nbb-logseq -cp script:src \
  script/get_task_info.cljs \
  "6985659e-59b1-47e7-8661-ee790462f263"

# 3. Update status to Doing
./node_modules/.bin/nbb-logseq -cp script:src \
  script/update_task_status.cljs \
  "6985659e-59b1-47e7-8661-ee790462f263" \
  "Doing"

# 4. Mark as complete
./node_modules/.bin/nbb-logseq -cp script:src \
  script/complete_task.cljs \
  "6985659e-59b1-47e7-8661-ee790462f263"
```

### Querying Tasks

```bash
# List all DONE tasks
./node_modules/.bin/nbb-logseq -cp script:src \
  script/list_done_tasks_v2.cljs

# List all tasks (any status)
./node_modules/.bin/nbb-logseq -cp script:src \
  script/list_all_tasks.cljs

# List tasks on a specific page
./node_modules/.bin/nbb-logseq -cp script:src \
  script/list_page_tasks_api.cljs \
  "Feb 5th, 2026"
```

---

## File Organization

```
deps/graph-parser/
├── script/
│   ├── complete_task.cljs              # Mark task Done
│   ├── create_task_clean.cljs          # Create clean tasks
│   ├── update_task_status.cljs         # Update status
│   ├── get_task_info.cljs              # Get task info
│   ├── list_page_blocks.cljs           # List page blocks
│   ├── list_page_tasks_api.cljs        # List page tasks
│   ├── list_done_tasks_v2.cljs         # Query DONE tasks
│   ├── list_all_tasks.cljs             # Query all tasks
│   ├── query_status_tasks.cljs         # Query by status
│   ├── find_tasks.cljs                 # Find tasks
│   ├── find_block_by_title.cljs        # Find blocks
│   ├── list_journals.cljs              # List journals
│   ├── list_date_pages.cljs            # List date pages
│   └── list_recent_pages.cljs          # List recent pages
├── SUMMARY.md                           # Main reference doc
├── FILE_CLEANUP_PLAN.md                 # Cleanup guide
├── ESSENTIAL_FILES.md                   # This file
├── QUICKSTART.md
├── IMPLEMENTATION_SUMMARY.md
├── SOLUTION_SUMMARY.md
├── TASK_COMPLETION_TRACKING.md
└── TASK_TRACKING_QUICK_REFERENCE.md
```

---

## API Configuration

All API scripts use these settings:

```clojure
API URL: "http://localhost:12315/api"
Authorization: "Bearer whatever"
Content-Type: "application/json"

Request format:
{
  "method": "logseq.Editor.<methodName>",
  "args": [arg1, arg2, ...]
}
```

### Key API Methods

- `logseq.Editor.appendBlockInPage` - Create block
- `logseq.Editor.addBlockTag` - Add tag invisibly
- `logseq.Editor.upsertBlockProperty` - Set property
- `logseq.Editor.getBlock` - Get block details
- `logseq.Editor.getPageBlocksTree` - Get page blocks

### Critical: Property Names

Always use **qualified property names**:
- ✓ `"logseq.property/status"`
- ✓ `"logseq.property/priority"`
- ✗ `"status"` (creates plugin property)
- ✗ `"priority"` (creates plugin property)

---

## Database Configuration

Query scripts connect to SQLite databases:

```clojure
(require '[logseq.db.common.sqlite-cli :as sqlite-cli])

(def conn (sqlite-cli/open-db! "/path/to/database"))
(def db @conn)
```

Common database paths:
- `~/.logseq/graphs/logseq_db_mcp` - MCP graph
- Demo database - varies by system

---

## Next Steps

1. **Review** `FILE_CLEANUP_PLAN.md` to see what will be removed
2. **Run cleanup:** `./cleanup_files.sh`
3. **Verify:** Check `git status` after cleanup
4. **Reference:** Use `SUMMARY.md` for comprehensive documentation
5. **Quick lookup:** Use this file (`ESSENTIAL_FILES.md`) for script usage

---

## Getting Help

- **Comprehensive guide:** See `SUMMARY.md`
- **API details:** See `SUMMARY.md` → "HTTP API Reference"
- **Data model:** See `SUMMARY.md` → "Data Model Reference"
- **Cleanup details:** See `FILE_CLEANUP_PLAN.md`
