# MCP Logseq

Programmatic interaction with Logseq using SQLite database queries and HTTP API.

## Overview

This project provides scripts and tools for:
- **Creating and managing tasks** via Logseq HTTP API
- **Querying the graph database** using DataScript/DataLog
- **Automating workflows** with Logseq

Built for Logseq's modern SQLite-backed architecture.

## Quick Start

### Prerequisites

1. **Logseq Installation** - Requires a Logseq development installation at `/Users/slim/slimslenderslacks/logseq`
   - Provides nbb-logseq runtime (v1.2.173-feat-db-v31)
   - Provides Logseq source code dependencies
   - Provides better-sqlite3 native module

2. **Node.js** - Version 20.10.0 or higher

### Installation

```bash
cd ~/slimslenderslacks/mcp-logseq

# Install dependencies
npm install
```

This installs better-sqlite3 (native SQLite bindings). All other dependencies come from the Logseq installation.

### Running Scripts

From the project root, use the `run-script.sh` wrapper:

```bash
# Query all tasks (defaults to 'mcp' graph)
./run-script.sh list_all_tasks.cljs

# Query tasks from a specific graph
./run-script.sh list_all_tasks.cljs Demo
./run-script.sh list_all_tasks.cljs mcp

# List tasks by status
./run-script.sh list_tasks_by_status.cljs mcp

# Create a task (API-based, doesn't need graph param)
./run-script.sh create_task_clean.cljs \
  "Feb 5th, 2026" \
  "My new task" \
  "Todo" \
  "High"

# Mark task as complete
./run-script.sh complete_task.cljs <uuid>

# Update task status
./run-script.sh update_task_status.cljs <uuid> "Doing"

# List available scripts
./run-script.sh
```

**Graph Parameter:**
- Database query scripts accept an optional graph name as the first parameter
- Graph name defaults to `"mcp"` if not specified
- Graphs are located at `~/logseq/graphs/<graph-name>/db.sqlite`
- Examples: `mcp`, `Demo`, `my-graph`

**Note:** The wrapper script uses the Logseq installation at `/Users/slim/slimslenderslacks/logseq` for dependencies.

## Project Structure

```
mcp-logseq/
├── scripts/              # ClojureScript scripts
│   ├── complete_task.cljs
│   ├── create_task_clean.cljs
│   ├── update_task_status.cljs
│   ├── get_task_info.cljs
│   ├── list_page_blocks.cljs
│   ├── list_page_tasks_api.cljs
│   ├── list_done_tasks_v2.cljs
│   ├── list_all_tasks.cljs
│   ├── query_status_tasks.cljs
│   ├── find_tasks.cljs
│   ├── find_block_by_title.cljs
│   ├── list_journals.cljs
│   ├── list_date_pages.cljs
│   ├── list_recent_pages.cljs
│   └── db_import.cljs
├── docs/                 # Documentation
│   ├── SUMMARY.md        # Comprehensive reference
│   ├── ESSENTIAL_FILES.md
│   └── ...
└── README.md            # This file
```

## Scripts Overview

### Task Management (HTTP API)

**Create Tasks**
- `create_task_clean.cljs` - Create tasks with clean titles (no visible #Task)

**Update Tasks**
- `complete_task.cljs` - Mark tasks as Done
- `update_task_status.cljs` - Update to any status (Todo, Doing, Done, etc.)

**Query Tasks**
- `get_task_info.cljs` - Get detailed task information
- `list_page_blocks.cljs` - List all blocks on a page
- `list_page_tasks_api.cljs` - List tasks on a page

### Database Queries (DataScript)

**Task Queries**
- `list_done_tasks_v2.cljs` - Query DONE tasks with timestamps
- `list_all_tasks.cljs` - Query all tasks
- `query_status_tasks.cljs` - Query by status
- `find_tasks.cljs` - Find tasks by criteria

**Block & Page Queries**
- `find_block_by_title.cljs` - Find blocks by title
- `list_journals.cljs` - List journal pages
- `list_date_pages.cljs` - List date pages
- `list_recent_pages.cljs` - List recent pages

## HTTP API Configuration

Scripts connect to Logseq's HTTP API:

```clojure
API URL: "http://localhost:12315/api"
Authorization: "Bearer whatever"
```

**Critical:** Always use qualified property names:
- ✓ `"logseq.property/status"`
- ✓ `"logseq.property/priority"`
- ✗ `"status"` (creates plugin property)

## Database Configuration

Query scripts connect to SQLite databases at:
- `~/.logseq/graphs/<graph-name>`

Example:
```clojure
(require '[logseq.db.common.sqlite-cli :as sqlite-cli])

(def conn (sqlite-cli/open-db! "~/.logseq/graphs/my_graph"))
(def db @conn)
```

## Example Workflows

### Create and Complete a Task

```bash
# 1. Create task
nbb-logseq -cp scripts scripts/create_task_clean.cljs \
  "Feb 5th, 2026" \
  "Implement authentication" \
  "Todo" \
  "High"
# Returns: UUID: 6985659e-59b1-47e7-8661-ee790462f263

# 2. Check task details
nbb-logseq -cp scripts scripts/get_task_info.cljs \
  "6985659e-59b1-47e7-8661-ee790462f263"

# 3. Update to Doing
nbb-logseq -cp scripts scripts/update_task_status.cljs \
  "6985659e-59b1-47e7-8661-ee790462f263" \
  "Doing"

# 4. Mark complete
nbb-logseq -cp scripts scripts/complete_task.cljs \
  "6985659e-59b1-47e7-8661-ee790462f263"
```

### Query Tasks

```bash
# List all done tasks
nbb-logseq -cp scripts scripts/list_done_tasks_v2.cljs

# List all tasks on today's journal
nbb-logseq -cp scripts scripts/list_page_tasks_api.cljs "Feb 5th, 2026"

# Find all high-priority tasks
nbb-logseq -cp scripts scripts/find_tasks.cljs
```

## Documentation

See `docs/` directory:
- **SUMMARY.md** - Comprehensive reference with API docs, data model, examples
- **ESSENTIAL_FILES.md** - Quick reference for all scripts
- **FILE_CLEANUP_PLAN.md** - History of file cleanup

## Key Concepts

### Task Structure

A proper Logseq task requires:
1. **Task tag** (entity 151) - Added invisibly via `addBlockTag`
2. **Status property** - Reference to status entity (77=Todo, 78=Doing, 80=Done)
3. **Priority property** - Reference to priority entity (83=Low, 84=Medium, 85=High)

### Data Model

Logseq stores data as **datoms** (entity-attribute-value-transaction):
```clojure
[256 :block/uuid #uuid "..." 262]
[256 :block/title "Task content" 262]
[256 :block/tags 151 262]  ; Task class
[256 :logseq.property/status 80 262]  ; Done
[256 :logseq.property/priority 85 262]  ; High
```

### Best Practices

1. **Use HTTP API for modifications** when Logseq is running
2. **Use qualified property names** (`logseq.property/status` not `status`)
3. **Never use `updateBlock` for tasks** - use `upsertBlockProperty` instead
4. **Add tags invisibly** with `addBlockTag` for clean titles

## Development

### Dependency Model

This project uses the **parent Logseq installation** for both runtime and source dependencies:

**Advantages:**
- ✓ No code duplication (saves 111MB+)
- ✓ Always uses latest Logseq source
- ✓ Single source of truth
- ✓ Easy to maintain
- ✓ Uses Logseq's exact nbb-logseq build (with feat-db-v31)

**How it works:**
- `run-script.sh` uses Logseq's nbb-logseq runtime (v1.2.173-feat-db-v31)
- Adds Logseq source directories to classpath
- Scripts access: `logseq.db.common.sqlite-cli`, `datascript.core`, etc.

**Why not use npm's @logseq/nbb-logseq?**
- Logseq uses a custom build (`1.2.173-feat-db-v31`) not published to npm
- This version includes database features (feat-db-v31) needed for SQLite access
- Standard npm version (1.2.173) lacks these features

**Required Logseq installation structure:**
```
/Users/slim/slimslenderslacks/logseq/
├── deps/
│   ├── cli/src
│   ├── common/src
│   ├── db/src
│   ├── graph-parser/
│   │   ├── src
│   │   └── node_modules/.bin/nbb-logseq (v1.2.173-feat-db-v31)
│   └── outliner/src
└── src/
```

### Adding New Scripts

1. Create script in `scripts/` directory
2. Add shebang: `#!/usr/bin/env nbb`
3. Include namespace and requires
4. Make executable: `chmod +x scripts/your_script.cljs`
5. Test with: `./run-script.sh your_script.cljs`

### Testing

Test scripts against Demo database or create a test graph.

## Resources

- **Logseq API Docs:** https://plugins-doc.logseq.com/
- **DataScript:** https://github.com/tonsky/datascript
- **nbb-logseq:** https://github.com/logseq/nbb-logseq

## Docker Support

See [DOCKER.md](DOCKER.md) for instructions on building and running mcp-logseq in Docker.

Quick start:
```bash
# Build image
docker build -t mcp-logseq .

# Run script
./docker-run.sh list_all_tasks.cljs mcp
```

## License

MIT

## Contributing

Contributions welcome! Please ensure:
- Scripts use HTTP API for modifications
- Database queries are read-only
- Documentation is updated
- Scripts are tested
