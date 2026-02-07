# File Cleanup Plan

## Summary

Removing **55 files** that are no longer needed:
- 14 transit-related files (old format)
- 7 direct database transaction scripts (using API instead)
- 3 file graph conversion files
- 4 superseded API scripts
- 27 experimental/debugging files

Keeping **20 essential files**:
- 6 API scripts for task management
- 8 database query scripts
- 6 documentation files

---

## Files to Remove

### Transit-related (14 files)
**Reason:** No longer using transit format, only SQLite DB

```
QUICKSTART_TRANSIT.md
REBUILD_TRANSIT.md
script/build_transit.cljs
script/compare_transit_structure.cljs
script/db_to_transit.cljs
script/test_query_transit.cljs
script/test_read_transit.cljs
script/test_initial_data_schema.cljs
script/test_schema_serialization.cljs
script/test_schema_source.cljs
script/test_serialize.cljs
script/test_serialize_roundtrip.cljs
scripts/rebuild-transit-simple.cljs
scripts/rebuild-transit.cljs
```

### Direct Database Transaction Scripts (7 files)
**Reason:** Using HTTP API for all modifications instead of direct DB transactions

```
script/add_block_to_page.cljs
script/add_file_to_db.cljs
script/add_high_priority_task.cljs
script/delete_page.cljs
script/preview_delete_page.cljs
script/test_idempotency.cljs
script/test_persist.cljs
```

### File Graph Conversion (3 files)
**Reason:** No longer converting from file-based graphs

```
FILE_GRAPH_TO_DB.md
deps/cli/src/logseq/cli/commands/import_file_graph.cljs
scripts/file-graph-to-db.cljs
```

### Superseded API Scripts (4 files)
**Reason:** Replaced by better versions (create_task_clean.cljs)

```
script/create_task_api.cljs
script/create_task_api_only.cljs
script/add_block_via_api.cljs
script/create_task_via_api_hybrid.sh
```

### Experimental/Debugging Files (27 files)
**Reason:** Used only for exploration, not needed for production use

```
script/check_block_tags.cljs
script/check_cooking_schema_version.cljs
script/check_kv_entities.cljs
script/check_mcp_schema_version.cljs
script/check_schema.cljs
script/check_sqlite_entities.cljs
script/check_status_values.cljs
script/check_storage.cljs
script/compare_blocks.cljs
script/compare_dbs.cljs
script/debug_datom_loss.cljs
script/debug_page_blocks.cljs
script/inspect_block.cljs
script/inspect_page.cljs
script/inspect_task_block.cljs
script/show_all_blocks_detailed.cljs
script/show_block_datoms.cljs
script/show_datoms.cljs
script/show_page_content.cljs
script/show_page_tasks.cljs
script/show_task_blocks.cljs
script/verify_sqlite_db.cljs
script/fresh_query.cljs
script/find_done_tasks.cljs
script/query_done_tasks.cljs
script/list_done_tasks.cljs
script/list_all_pages.cljs
```

---

## Files to Keep

### Essential API Scripts (6 files)
**Purpose:** Task management via HTTP API

```
script/complete_task.cljs          # Mark tasks as Done
script/create_task_clean.cljs      # Create tasks with clean titles
script/update_task_status.cljs     # Update task status
script/get_task_info.cljs          # Get task details
script/list_page_blocks.cljs       # List blocks on a page
script/list_page_tasks_api.cljs    # List tasks on a page
```

### Essential Database Query Scripts (8 files)
**Purpose:** Query graph database for tasks and pages

```
script/list_done_tasks_v2.cljs     # Query DONE tasks (primary version)
script/list_all_tasks.cljs         # Query all tasks
script/query_status_tasks.cljs     # Query tasks by status
script/find_tasks.cljs             # Find tasks by criteria
script/find_block_by_title.cljs    # Find blocks by title
script/list_journals.cljs          # List journal pages
script/list_date_pages.cljs        # List date-based pages
script/list_recent_pages.cljs      # List recently modified pages
```

### Documentation (6 files)
**Purpose:** Reference and guides

```
SUMMARY.md                         # Comprehensive reference (just created)
QUICKSTART.md                      # Getting started guide
IMPLEMENTATION_SUMMARY.md          # Implementation details
SOLUTION_SUMMARY.md                # Solution overview
TASK_COMPLETION_TRACKING.md        # Task tracking guide
TASK_TRACKING_QUICK_REFERENCE.md   # Quick reference
```

---

## Cleanup Command

To remove all unnecessary files, run:

```bash
cd /Users/slim/slimslenderslacks/logseq/deps/graph-parser

# Remove transit-related files
rm ../../QUICKSTART_TRANSIT.md
rm ../../REBUILD_TRANSIT.md
rm script/build_transit.cljs
rm script/compare_transit_structure.cljs
rm script/db_to_transit.cljs
rm script/test_query_transit.cljs
rm script/test_read_transit.cljs
rm script/test_initial_data_schema.cljs
rm script/test_schema_serialization.cljs
rm script/test_schema_source.cljs
rm script/test_serialize.cljs
rm script/test_serialize_roundtrip.cljs
rm ../../scripts/rebuild-transit-simple.cljs
rm ../../scripts/rebuild-transit.cljs

# Remove direct DB transaction scripts
rm script/add_block_to_page.cljs
rm script/add_file_to_db.cljs
rm script/add_high_priority_task.cljs
rm script/delete_page.cljs
rm script/preview_delete_page.cljs
rm script/test_idempotency.cljs
rm script/test_persist.cljs

# Remove file graph conversion files
rm ../../FILE_GRAPH_TO_DB.md
rm ../cli/src/logseq/cli/commands/import_file_graph.cljs
rm ../../scripts/file-graph-to-db.cljs

# Remove superseded API scripts
rm script/create_task_api.cljs
rm script/create_task_api_only.cljs
rm script/add_block_via_api.cljs
rm script/create_task_via_api_hybrid.sh

# Remove experimental/debugging files
rm script/check_block_tags.cljs
rm script/check_cooking_schema_version.cljs
rm script/check_kv_entities.cljs
rm script/check_mcp_schema_version.cljs
rm script/check_schema.cljs
rm script/check_sqlite_entities.cljs
rm script/check_status_values.cljs
rm script/check_storage.cljs
rm script/compare_blocks.cljs
rm script/compare_dbs.cljs
rm script/debug_datom_loss.cljs
rm script/debug_page_blocks.cljs
rm script/inspect_block.cljs
rm script/inspect_page.cljs
rm script/inspect_task_block.cljs
rm script/show_all_blocks_detailed.cljs
rm script/show_block_datoms.cljs
rm script/show_datoms.cljs
rm script/show_page_content.cljs
rm script/show_page_tasks.cljs
rm script/show_task_blocks.cljs
rm script/verify_sqlite_db.cljs
rm script/fresh_query.cljs
rm script/find_done_tasks.cljs
rm script/query_done_tasks.cljs
rm script/list_done_tasks.cljs
rm script/list_all_pages.cljs

echo "✓ Removed 55 unnecessary files"
echo "✓ Keeping 20 essential files"
```

---

## File Count Summary

**Before cleanup:** 75+ uncommitted files
**After cleanup:** 20 essential files
**Removed:** 55 files (73% reduction)

### Breakdown of kept files:
- 6 API scripts (task management)
- 8 database query scripts
- 6 documentation files

All kept files are actively used and serve clear purposes for SQLite DB + HTTP API workflow.
