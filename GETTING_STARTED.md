# Getting Started with MCP Logseq

## Installation

```bash
cd ~/slimslenderslacks/mcp-logseq

# Install dependencies
npm install

# Note: Scripts use the run-script.sh wrapper which leverages
# the Logseq installation for nbb-logseq and source dependencies
```

## First Steps

### 1. Verify Logseq is Running

Ensure Logseq is running with HTTP API enabled on `http://localhost:12315`

### 2. Test Creating a Task

```bash
./run-script.sh create_task_clean.cljs \
  "$(date +'%b %-d, %Y')" \
  "Test task from MCP" \
  "Todo" \
  "High"
```

This will:
- Create a task on today's journal page
- Set status to "Todo"
- Set priority to "High"
- Return a UUID for the task

### 3. Check Task Info

Using the UUID from step 2:

```bash
./run-script.sh get_task_info.cljs "<uuid>"
```

### 4. Update Task Status

```bash
# Mark as Doing
./run-script.sh update_task_status.cljs "<uuid>" "Doing"

# Mark as Done
./run-script.sh complete_task.cljs "<uuid>"
```

### 5. Query Tasks

```bash
# List all tasks in the mcp graph
./run-script.sh list_all_tasks.cljs

# List tasks on a page
./run-script.sh list_page_tasks_api.cljs "Feb 5th, 2026"
```

## Quick Reference

### Task Status Values
- `"Todo"` - Not started
- `"Doing"` - In progress
- `"Done"` - Completed
- `"Later"` - Deferred
- `"Now"` - Current focus
- `"Waiting"` - Blocked
- `"Canceled"` - Abandoned

### Priority Values
- `"High"`
- `"Medium"`
- `"Low"`

## Configuration

### Database Paths

Edit scripts to point to your graph database:

```clojure
; Default in list_done_tasks_v2.cljs:
(def db-path "~/.logseq/graphs/logseq_db_mcp")

; Update to your graph:
(def db-path "~/.logseq/graphs/your_graph_name")
```

### API Endpoint

All API scripts use:
```clojure
URL: "http://localhost:12315/api"
Auth: "Bearer whatever"
```

Change if your Logseq uses a different port or token.

## Common Workflows

### Daily Task Management

```bash
# Morning: Create today's tasks
TODAY="$(date +'%b %-d, %Y')"

nbb-logseq -cp scripts scripts/create_task_clean.cljs \
  "$TODAY" "Review emails" "Todo" "High"

nbb-logseq -cp scripts scripts/create_task_clean.cljs \
  "$TODAY" "Team meeting" "Todo" "Medium"

# During day: Update status
nbb-logseq -cp scripts scripts/update_task_status.cljs "<uuid>" "Doing"

# Evening: Complete tasks
nbb-logseq -cp scripts scripts/complete_task.cljs "<uuid>"
```

### Weekly Review

```bash
# Query all done tasks
nbb-logseq -cp scripts scripts/list_done_tasks_v2.cljs

# List recent pages
nbb-logseq -cp scripts scripts/list_recent_pages.cljs

# Find incomplete tasks
nbb-logseq -cp scripts scripts/find_tasks.cljs
```

## Troubleshooting

### "Cannot connect to API"

1. Verify Logseq is running
2. Check API port: `http://localhost:12315/api`
3. Enable HTTP API in Logseq settings if needed

### "Database not found"

1. Check database path in script
2. Verify graph exists: `ls ~/.logseq/graphs/`
3. Update `db-path` in scripts to match your graph

### "Property not found"

Ensure you're using qualified property names:
- ✓ `"logseq.property/status"`
- ✗ `"status"`

## Next Steps

1. Read `docs/SUMMARY.md` for comprehensive documentation
2. Explore `docs/ESSENTIAL_FILES.md` for script reference
3. Check `README.md` for project overview
4. Customize scripts for your workflow

## Help

- **Full API reference:** `docs/SUMMARY.md` → "HTTP API Reference"
- **Data model:** `docs/SUMMARY.md` → "Data Model Reference"
- **Script usage:** `docs/ESSENTIAL_FILES.md`

Happy automating! 🚀
