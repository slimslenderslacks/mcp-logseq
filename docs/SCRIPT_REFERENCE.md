# Script Reference

## Database Query Scripts (11 scripts)

These scripts query the SQLite database directly using DataScript/DataLog queries.

**Usage:** `./run-script.sh <script-name> [graph-name] [additional-args]`

### Task Queries

**`list_all_tasks.cljs`**
- **Purpose:** List all tasks with their status and priority
- **Usage:** `./run-script.sh list_all_tasks.cljs [graph-name]`
- **Default graph:** `mcp`
- **Output:** Task ID, UUID, title, status, priority

**`list_tasks_by_status.cljs`**
- **Purpose:** Group tasks by status (Todo, Doing, Done, Backlog)
- **Usage:** `./run-script.sh list_tasks_by_status.cljs [graph-name]`
- **Default graph:** `mcp`
- **Output:** Tasks organized by status with counts

**`list_done_tasks_v2.cljs`**
- **Purpose:** List completed tasks with timestamps and duration
- **Usage:** `./run-script.sh list_done_tasks_v2.cljs [graph-name]`
- **Default graph:** `mcp`
- **Output:** Task title, page, created/completed dates, duration

**`find_tasks.cljs`**
- **Purpose:** Find tasks by various criteria
- **Usage:** `./run-script.sh find_tasks.cljs [graph-name]`
- **Default graph:** `mcp`

**`query_status_tasks.cljs`**
- **Purpose:** Query tasks by specific status
- **Usage:** `./run-script.sh query_status_tasks.cljs [graph-name]`
- **Default graph:** `mcp`

### Block & Page Queries

**`find_block_by_title.cljs`**
- **Purpose:** Find a block by title and show all properties
- **Usage:** `./run-script.sh find_block_by_title.cljs <graph-name> <page-name> <title>`
- **Example:** `./run-script.sh find_block_by_title.cljs mcp "Feb 6th, 2026" "my task"`

**`list_journals.cljs`**
- **Purpose:** List all journal pages
- **Usage:** `./run-script.sh list_journals.cljs [graph-name]`
- **Default graph:** `mcp`

**`list_date_pages.cljs`**
- **Purpose:** List pages organized by date
- **Usage:** `./run-script.sh list_date_pages.cljs [graph-name]`
- **Default graph:** `mcp`

**`list_recent_pages.cljs`**
- **Purpose:** List recently modified pages
- **Usage:** `./run-script.sh list_recent_pages.cljs [graph-name] [limit]`
- **Default graph:** `mcp`
- **Default limit:** `10`
- **Example:** `./run-script.sh list_recent_pages.cljs Demo 20`

### Utilities

**`debug_tasks.cljs`**
- **Purpose:** Debug task queries and entity IDs
- **Usage:** `./run-script.sh debug_tasks.cljs [graph-name]`
- **Default graph:** `mcp`
- **Output:** Task class entity, blocks with tags, blocks with status

**`db_import.cljs`**
- **Purpose:** Import data into database
- **Usage:** `./run-script.sh db_import.cljs [graph-name]`
- **Default graph:** `mcp`

---

## API Scripts (6 scripts)

These scripts use the Logseq HTTP API at `http://localhost:12315/api`.

**Requirements:**
- Logseq must be running
- HTTP API must be enabled
- Default auth token: `"whatever"`

### Task Management

**`create_task_clean.cljs`** ⭐ Recommended
- **Purpose:** Create tasks with clean titles (no visible #Task tag)
- **Usage:** `./run-script.sh create_task_clean.cljs <page-name> <content> [status] [priority]`
- **Default status:** `Todo`
- **Default priority:** `High`
- **Example:**
  ```bash
  ./run-script.sh create_task_clean.cljs \
    "Feb 6th, 2026" \
    "Implement feature X" \
    "Todo" \
    "High"
  ```
- **Returns:** Block ID, UUID
- **Note:** Task appears immediately in Logseq

**`complete_task.cljs`**
- **Purpose:** Mark a task as Done
- **Usage:** `./run-script.sh complete_task.cljs <uuid>`
- **Example:** `./run-script.sh complete_task.cljs "6985211f-77e3-4cf0-b768-cc49f415a2b6"`
- **Note:** Preserves all other properties (tags, priority)

**`update_task_status.cljs`**
- **Purpose:** Update task status to any value
- **Usage:** `./run-script.sh update_task_status.cljs <uuid> <status>`
- **Status options:** `Todo`, `Doing`, `Done`, `Later`, `Now`, `Waiting`, `Canceled`, `In Review`, `Backlog`
- **Example:** `./run-script.sh update_task_status.cljs "<uuid>" "Doing"`

### Task Information

**`get_task_info.cljs`**
- **Purpose:** Get detailed task information
- **Usage:** `./run-script.sh get_task_info.cljs <uuid>`
- **Output:** Content, status, priority, full block data (JSON)
- **Example:** `./run-script.sh get_task_info.cljs "6985211f-77e3-4cf0-b768-cc49f415a2b6"`

**`list_page_blocks.cljs`**
- **Purpose:** List all blocks on a page
- **Usage:** `./run-script.sh list_page_blocks.cljs [page-name]`
- **Default page:** `"Feb 5th, 2026"`
- **Output:** UUID, content, properties for each block

**`list_page_tasks_api.cljs`**
- **Purpose:** List tasks on a specific page with properties
- **Usage:** `./run-script.sh list_page_tasks_api.cljs [page-name]`
- **Default page:** `"Feb 5th, 2026"`
- **Output:** Tasks with status and priority

---

## Quick Reference Table

| Script | Type | Graph Param | Primary Use |
|--------|------|-------------|-------------|
| `list_all_tasks.cljs` | Database | ✓ | View all tasks |
| `list_tasks_by_status.cljs` | Database | ✓ | Tasks by status |
| `list_done_tasks_v2.cljs` | Database | ✓ | Done tasks with timing |
| `find_tasks.cljs` | Database | ✓ | Search tasks |
| `query_status_tasks.cljs` | Database | ✓ | Query by status |
| `find_block_by_title.cljs` | Database | ✓ | Find specific block |
| `list_journals.cljs` | Database | ✓ | List journal pages |
| `list_date_pages.cljs` | Database | ✓ | Date-based pages |
| `list_recent_pages.cljs` | Database | ✓ | Recent pages |
| `debug_tasks.cljs` | Database | ✓ | Debug queries |
| `db_import.cljs` | Database | ✓ | Import data |
| `create_task_clean.cljs` | API | ✗ | **Create tasks** |
| `complete_task.cljs` | API | ✗ | **Mark done** |
| `update_task_status.cljs` | API | ✗ | **Change status** |
| `get_task_info.cljs` | API | ✗ | Task details |
| `list_page_blocks.cljs` | API | ✗ | Blocks on page |
| `list_page_tasks_api.cljs` | API | ✗ | Tasks on page |

---

## When to Use Each Approach

### Use Database Queries When:
- ✓ Querying across entire graph
- ✓ Complex DataLog queries needed
- ✓ Analyzing patterns or statistics
- ✓ Logseq can be closed
- ✓ Read-only operations
- ✓ Need to query historical data

### Use HTTP API When:
- ✓ Creating or modifying content
- ✓ Logseq must be running
- ✓ Need immediate UI updates
- ✓ Working with current session
- ✓ Real-time interactions
- ✓ Simple CRUD operations

---

## Common Workflows

### View All Tasks in MCP Graph
```bash
./run-script.sh list_tasks_by_status.cljs mcp
```

### View Tasks in Demo Graph
```bash
./run-script.sh list_all_tasks.cljs Demo
```

### Create and Complete a Task
```bash
# 1. Create
./run-script.sh create_task_clean.cljs \
  "Feb 6th, 2026" \
  "New feature" \
  "Todo" \
  "High"
# Returns UUID: 6985211f-xxxx-xxxx-xxxx-xxxxxxxxxxxx

# 2. Update to Doing
./run-script.sh update_task_status.cljs \
  "6985211f-xxxx-xxxx-xxxx-xxxxxxxxxxxx" \
  "Doing"

# 3. Mark complete
./run-script.sh complete_task.cljs \
  "6985211f-xxxx-xxxx-xxxx-xxxxxxxxxxxx"

# 4. Verify
./run-script.sh get_task_info.cljs \
  "6985211f-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
```

### Weekly Review
```bash
# See all done tasks
./run-script.sh list_done_tasks_v2.cljs mcp

# See current workload
./run-script.sh list_tasks_by_status.cljs mcp

# Check recent activity
./run-script.sh list_recent_pages.cljs mcp 20
```

---

## Notes

- **Default Graph:** Most database query scripts default to `"mcp"` if no graph is specified
- **Graph Location:** `~/logseq/graphs/<graph-name>/db.sqlite`
- **API Endpoint:** `http://localhost:12315/api`
- **Auth Token:** `"Bearer whatever"` (local development)
- **Property Names:** Always use qualified names like `"logseq.property/status"`

## See Also

- [DATALOG_PATTERNS.md](DATALOG_PATTERNS.md) - DataLog query patterns and best practices
- [SUMMARY.md](SUMMARY.md) - Comprehensive reference documentation
- [ESSENTIAL_FILES.md](ESSENTIAL_FILES.md) - Quick file reference
