# Logseq Database Interaction Summary

## Overview

This document summarizes our exploration of Logseq's database-backed architecture, including querying the graph database, creating and managing tasks via the HTTP API, and understanding the underlying data model.

## Logseq's Database-Backed Model

### Architecture

Modern Logseq uses a **SQLite database** with a **DataScript storage layer** for all graphs. This replaced the older file-based `.transit` format:

- Database location: `~/.logseq/graphs/<graph-name>`
- Storage format: SQLite with DataScript datoms
- Persistence: Changes must be explicitly stored using `(datascript.storage/store @conn)`
- Concurrent access: Direct database modifications are overwritten when Logseq is running - use HTTP API instead

### DataScript Datoms

DataScript stores data as **datoms** (data atoms) - tuples of:
```clojure
[entity-id attribute value transaction-id]
```

Example datoms for a task block:
```clojure
[256 :block/uuid #uuid "69855f7c-2461-4158-98bd-b26434537654" 262]
[256 :block/title "Clean high priority task" 262]
[256 :block/tags 151 262]  ; 151 = Task class entity
[256 :logseq.property/status 80 262]  ; 80 = Done entity
[256 :logseq.property/priority 85 262]  ; 85 = High entity
[256 :block/page 171 262]  ; 171 = page entity
[256 :block/parent 171 262]
```

## Use Cases Explored

### 1. Creating SQLite Database from Markdown Files
**Goal:** Convert markdown files to SQLite database for MCP graph integration

**Scripts:**
- Initial exploration created databases with 25,026 datoms from 87 files

**Key Learning:** Logseq now exclusively uses SQLite format; the old file-based `.transit` format is deprecated.

---

### 2. Querying Tasks with DataLog

**Goal:** Query for completed (DONE) tasks using DataScript/DataLog queries

**Script:** `script/list_done_tasks_v2.cljs`

**Query Structure:**
```clojure
(d/q '[:find (pull ?b [:block/title
                        :block/created-at
                        :block/updated-at
                        {:logseq.property/status [:block/title :db/ident]}
                        {:block/page [:block/title]}])
       :where
       [?b :logseq.property/status ?status]
       [?status :db/ident :logseq.property/status.done]]
     db)
```

**Results:** Found 156 DONE tasks in Demo database

**Key Concepts:**
- Modern Logseq uses **property-based status** (`:logseq.property/status`)
- Old Logseq used **marker-based status** (`:block/marker "DONE"`)
- Status is an entity reference, not a string value
- Pull patterns allow fetching nested data in one query

---

### 3. Adding Files to Existing Database

**Goal:** Insert markdown files into existing SQLite database

**Script:** `script/add_file_to_db.cljs`

**Idempotency:** Partial
- Pages: Idempotent (can re-add without duplicates)
- Blocks without UUIDs: Not idempotent (creates duplicates)

**Critical Discovery:** Must call `(datascript.storage/store @conn)` after transactions to persist changes.

---

### 4. Deleting Pages and Blocks

**Goal:** Recursively delete pages and their blocks from database

**Scripts:**
- `script/delete_page.cljs` - Executes deletion
- `script/preview_delete_page.cljs` - Preview before deletion

**Process:**
1. Find page by name
2. Find all blocks belonging to page
3. Retract all blocks
4. Retract page entity
5. Store changes

**Key Learning:** Extracting page ID from query results requires careful handling:
```clojure
; Correct
(and (seq page-result) (:db/id (ffirst page-result)))

; Incorrect (causes "is not ISeqable" error)
(ffirst (map #(:db/id (first %)) page-result))
```

---

### 5. Exploring Journal Pages

**Goal:** Examine journal page structure and content

**Database Queries:**
```clojure
; Find journal pages
(d/q '[:find (pull ?b [:block/title])
       :where
       [?b :block/journal-day ?day]]
     db)

; Get all datoms for a specific block
(d/datoms db :eavt block-id)
```

**Findings:**
- Journal pages have `:block/journal-day` attribute
- Each block has multiple datoms describing its properties
- References (`:block/refs`) link to other entities

---

### 6. Creating Tasks via HTTP API

**Goal:** Programmatically create tasks with proper status and priority

**Final Solution:** `script/create_task_clean.cljs`

**API Workflow:**
```clojure
1. Create block: (api-call "logseq.Editor.appendBlockInPage"
                           [page-name content])
2. Add Task tag invisibly: (api-call "logseq.Editor.addBlockTag"
                                    [uuid "Task"])
3. Set status: (api-call "logseq.Editor.upsertBlockProperty"
                        [uuid "logseq.property/status" status])
4. Set priority: (api-call "logseq.Editor.upsertBlockProperty"
                          [uuid "logseq.property/priority" priority])
```

**Critical Discovery:** Must use **qualified property names**:
- ✓ Correct: `"logseq.property/status"`, `"logseq.property/priority"`
- ✗ Incorrect: `"status"`, `"priority"` (creates plugin properties with `_test_plugin` prefix)

---

### 7. Updating Task Status

**Goal:** Mark tasks as complete or change their status

**Scripts:**
- `script/complete_task.cljs` - Marks tasks as Done
- `script/update_task_status.cljs` - Changes status to any value

**Solution:**
```clojure
(api-call "logseq.Editor.upsertBlockProperty"
         [uuid "logseq.property/status" "Done"])
```

**Critical Discovery:** Never use `updateBlock` for tasks:
- `updateBlock` replaces entire content, destroying structured properties
- Always use `upsertBlockProperty` to update individual properties

---

## HTTP API Reference

### Base Configuration
```clojure
API URL: "http://localhost:12315/api"
Authorization: "Bearer whatever"
Content-Type: "application/json"
```

### API Methods Used

#### 1. Page Operations

**Create Page:**
```clojure
Method: "logseq.Editor.createPage"
Args: [page-name properties options]
```

**Get Page Blocks:**
```clojure
Method: "logseq.Editor.getPageBlocksTree"
Args: [page-name]
Returns: Array of block objects with content, uuid, properties
```

#### 2. Block Operations

**Append Block:**
```clojure
Method: "logseq.Editor.appendBlockInPage"
Args: [page-name content]
Returns: {:id block-id, :uuid uuid, :content content}
```

**Get Block:**
```clojure
Method: "logseq.Editor.getBlock"
Args: [uuid]
Returns: Full block object with properties, tags, refs
```

**Update Block Content (AVOID for tasks):**
```clojure
Method: "logseq.Editor.updateBlock"
Args: [uuid new-content]
Warning: Destroys structured properties (tags, status, priority)
```

#### 3. Property Operations

**Upsert Property:**
```clojure
Method: "logseq.Editor.upsertBlockProperty"
Args: [uuid property-name value]

; IMPORTANT: Use qualified property names
Examples:
  [uuid "logseq.property/status" "Done"]
  [uuid "logseq.property/priority" "High"]
```

**Get Property:**
```clojure
Method: "logseq.Editor.getBlockProperty"
Args: [uuid property-name]
Returns: Property value object or entity
```

#### 4. Tag Operations

**Add Tag Invisibly:**
```clojure
Method: "logseq.Editor.addBlockTag"
Args: [uuid tag-name]
Note: Tag doesn't appear in block content but is stored in :block/tags
```

**Get All Tags:**
```clojure
Method: "logseq.Editor.getAllTags"
Args: [uuid]
Warning: Returns all available class tags, not just block-specific tags
```

---

## Database Querying

### Opening Database Connection

```clojure
(require '[logseq.db.common.sqlite-cli :as sqlite-cli])

(def conn (sqlite-cli/open-db! "/path/to/graph/database"))
(def db @conn)
```

### DataLog Query Patterns

#### Basic Entity Lookup
```clojure
; Find entities with specific attribute
[:find ?e
 :where
 [?e :block/title ?title]]
```

#### Pull Pattern (Fetch Related Data)
```clojure
; Get block with nested status entity
[:find (pull ?b [:block/title
                 {:logseq.property/status [:block/title :db/ident]}])
 :where
 [?b :logseq.property/status ?status]]
```

#### Entity Identity Filtering
```clojure
; Find tasks with Done status
[:find ?b
 :where
 [?b :logseq.property/status ?status]
 [?status :db/ident :logseq.property/status.done]]
```

#### Multiple Joins
```clojure
; Find high priority tasks on specific page
[:find (pull ?b [:block/title])
 :where
 [?b :block/tags 151]  ; Task class
 [?b :logseq.property/priority 85]  ; High priority
 [?b :block/page ?page]
 [?page :block/title "Feb 5th, 2026"]]
```

### Direct Datom Access

```clojure
; Get all datoms for an entity
(d/datoms db :eavt entity-id)

; Get all blocks on a page
(d/q '[:find (pull ?b [:db/id :block/uuid :block/title])
       :where
       [?b :block/page ?page]
       [?page :block/title "Page Name"]]
     db)
```

---

## Data Model Reference

### Core Entities

#### Block Entity Structure
```clojure
{:db/id 256
 :block/uuid #uuid "69855f7c-2461-4158-98bd-b26434537654"
 :block/title "Task content"
 :block/content "Task content"  ; Same as title for simple blocks
 :block/tags [151]  ; References to tag entities
 :block/refs [22 75 82 151]  ; All referenced entities
 :block/page {:id 171}  ; Parent page
 :block/parent {:id 171}  ; Parent block or page
 :block/created-at 1770349982795
 :block/updated-at 1770349987846
 :logseq.property/status {:id 80}  ; Status entity
 :logseq.property/priority {:id 85}}  ; Priority entity
```

#### Page Entity
```clojure
{:db/id 171
 :block/title "Feb 5th, 2026"
 :block/journal-day 20260205  ; For journal pages
 :block/type "journal"}
```

### Built-in Property Entities

#### Status Property (`:logseq.property/status`)
```clojure
Property entity: 75
Values:
  77 = Todo (:logseq.property/status.todo)
  78 = Doing (:logseq.property/status.doing)
  79 = Later (:logseq.property/status.later)
  80 = Done (:logseq.property/status.done)
  81 = Now (:logseq.property/status.now)
  166 = Waiting (:logseq.property/status.waiting)
  167 = Canceled (:logseq.property/status.canceled)
```

#### Priority Property (`:logseq.property/priority`)
```clojure
Property entity: 82
Values:
  83 = Low (:logseq.property/priority.low)
  84 = Medium (:logseq.property/priority.medium)
  85 = High (:logseq.property/priority.high)
```

#### Task Class (`:logseq.class/Task`)
```clojure
Entity: 151
UUID: 00000002-1282-1814-5700-000000000000
Properties: [75 82 87 89]  ; status, priority, deadline, scheduled
```

### Property Naming Conventions

**Built-in Properties:**
- Namespace: `logseq.property/`
- Examples: `logseq.property/status`, `logseq.property/priority`

**Plugin Properties:**
- Namespace: `plugin.property.<plugin-id>/`
- Example: `plugin.property._test_plugin/status` (created when using unqualified names)

**User Properties:**
- Can use any namespace
- Recommended: `user.property/` for custom properties

---

## Key Scripts Created

### Database Query Scripts

**`script/list_done_tasks_v2.cljs`**
- Lists all DONE tasks with timestamps
- Shows task completion duration
- Database path: `~/.logseq/graphs/logseq_db_mcp`

**`script/add_file_to_db.cljs`**
- Inserts markdown files into existing database
- Handles page and block creation
- Requires calling `storage/store` to persist

**`script/delete_page.cljs`**
- Recursively deletes page and all blocks
- `script/preview_delete_page.cljs` - Preview version

**`script/list_all_tasks.cljs`**
- Lists all tasks with status and priority
- Database path: Demo database

### HTTP API Scripts

**`script/create_task_clean.cljs`** ✓ Recommended
- Creates tasks with clean titles (no visible #Task)
- Sets status and priority
- Usage: `create_task_clean.cljs "Page Name" "Task content" [status] [priority]`

**`script/create_task_api.cljs`**
- Creates tasks with #Task visible in title
- Alternative to clean version

**`script/complete_task.cljs`** ✓ Recommended
- Marks tasks as Done
- Preserves all other properties
- Usage: `complete_task.cljs <block-uuid>`

**`script/update_task_status.cljs`** ✓ Recommended
- Updates task status to any value
- Usage: `update_task_status.cljs <block-uuid> <status>`

**`script/list_page_blocks.cljs`**
- Lists all blocks on a page via API
- Shows content and properties

**`script/get_task_info.cljs`**
- Gets detailed task information
- Shows status, priority, and full block data
- Usage: `get_task_info.cljs <block-uuid>`

### Utility Scripts

**`script/check_block_tags.cljs`**
- Checks tags and properties for all blocks on a page
- Useful for debugging

---

## Critical Discoveries & Best Practices

### 1. Property Naming
**Always use qualified property names with the HTTP API:**
```clojure
✓ "logseq.property/status"
✗ "status"  ; Creates plugin.property._test_plugin/status
```

### 2. Task Creation Requirements
A proper task requires:
1. **Task tag** (entity 151) - use `addBlockTag` to add invisibly
2. **Status property** - reference to status entity (77-81, 166-167)
3. **Priority property** - reference to priority entity (83-85)

### 3. Database Persistence
```clojure
; After any d/transact! operation
(datascript.storage/store @conn)
```

### 4. Concurrent Access
- **Database writes while Logseq is running:** Changes will be overwritten
- **Solution:** Use HTTP API for modifications when Logseq is open

### 5. Block Updates
**Never use `updateBlock` for structured content:**
- Replaces entire content
- Destroys tags, refs, and properties
- Use `upsertBlockProperty` for individual property updates

### 6. Tag Management
```clojure
; Visible tag (appears in content)
Content: "Task content #Task"

; Invisible tag (stored in :block/tags)
1. Create block without tag in content
2. Call addBlockTag to add tag invisibly
```

### 7. Query Result Handling
```clojure
; DataLog queries return vectors of vectors
; Example: [[{:db/id 134 :block/title "Page"}]]

; Safe extraction
(when-let [result (seq query-result)]
  (:db/id (ffirst result)))
```

---

## Example Workflows

### Creating and Completing a Task

```bash
# 1. Create a high-priority task
./node_modules/.bin/nbb-logseq -cp script:src \
  script/create_task_clean.cljs \
  "Feb 5th, 2026" \
  "Implement feature X" \
  "Todo" \
  "High"

# Output: UUID: 6985659e-59b1-47e7-8661-ee790462f263

# 2. Check task info
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

### Querying Tasks from Database

```clojure
#!/usr/bin/env nbb
(require '[datascript.core :as d])
(require '[logseq.db.common.sqlite-cli :as sqlite-cli])

(def conn (sqlite-cli/open-db! "~/.logseq/graphs/my_graph"))
(def db @conn)

; Find all high-priority tasks
(def high-priority-tasks
  (d/q '[:find (pull ?b [:block/title
                          {:logseq.property/status [:block/title]}
                          {:logseq.property/priority [:block/title]}])
         :where
         [?b :block/tags 151]  ; Task
         [?b :logseq.property/priority 85]]  ; High
       db))

; Find tasks in progress
(def doing-tasks
  (d/q '[:find (pull ?b [:block/title
                          {:block/page [:block/title]}])
         :where
         [?b :logseq.property/status 78]]  ; Doing
       db))
```

---

## Testing Results

### Task Management Scripts (Verified Working ✓)

**Test 1: Create and Complete**
- Created task: "Test task for completion" (Todo, High)
- UUID: `6985659e-59b1-47e7-8661-ee790462f263`
- Marked as Done using `complete_task.cljs`
- Result: Status = Done (entity 80), all properties preserved ✓

**Test 2: Status Updates**
- Created task: "Test status updates" (Todo, Medium)
- UUID: `698565bf-46a5-4dd1-9e1f-0fe4ad28fd3a`
- Updated: Todo → Doing → Done
- Result: All status transitions successful, properties intact ✓

**Verified:**
- Task tags preserved (entity 151) ✓
- Priority values maintained ✓
- Content remains clean (no DONE prefix) ✓
- References and metadata intact ✓

---

## Database Schema Insights

### Entity Types
1. **Blocks** - Basic content units (tasks, notes, headings)
2. **Pages** - Top-level containers (daily journals, named pages)
3. **Classes** - Type definitions (Task, Template, Query, Card)
4. **Properties** - Metadata attributes (status, priority, deadline)
5. **Property Values** - Closed value sets (Todo/Doing/Done, High/Medium/Low)

### Relationships
```
Page (171)
  ├─ Block 1 (256)
  │   ├─ :block/tags → Task Class (151)
  │   ├─ :logseq.property/status → Done (80)
  │   ├─ :logseq.property/priority → High (85)
  │   └─ :block/refs → [Page, Status, Priority, Task]
  └─ Block 2 (265)
      └─ ...
```

### Built-in Classes
```clojure
1   = Object (base class)
2   = Class (metaclass)
4   = Template
151 = Task
152 = Query
153 = Card
154 = Cards
156 = Code-block
157 = Quote-block
158 = Math-block
```

---

## Resources

### File Locations
- **Database:** `~/.logseq/graphs/<graph-name>`
- **Scripts:** `/Users/slim/slimslenderslacks/logseq/deps/graph-parser/script/`
- **API docs:** https://plugins-doc.logseq.com/

### API Configuration
- **Endpoint:** `http://localhost:12315/api`
- **Auth token:** `"whatever"` (for local development)
- **Method format:** POST with JSON body containing `method` and `args`

### Running Scripts
```bash
# From graph-parser directory
./node_modules/.bin/nbb-logseq -cp script:src script/<script-name>.cljs [args]
```

---

## Conclusion

This exploration revealed Logseq's modern database-backed architecture, enabling powerful programmatic interactions through both DataLog queries and HTTP API calls. Key achievements:

1. **Understanding the data model** - Entity-attribute-value structure with DataScript
2. **Creating tasks programmatically** - Clean task creation with proper properties
3. **Managing task lifecycle** - Status updates preserving task structure
4. **Querying the graph** - Complex DataLog queries for task analysis
5. **Best practices** - Qualified property names, API vs database access, property preservation

The combination of database queries for analysis and HTTP API for modifications provides a robust foundation for building automation tools and integrations with Logseq.
