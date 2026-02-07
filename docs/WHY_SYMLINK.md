# Why better-sqlite3 Must Be Local

**Note:** This document explains the technical reason why better-sqlite3 must be installed locally in mcp-logseq. We currently use `npm install` to install it. This document is kept for reference to explain the underlying Node.js module resolution behavior.

## The Question

**Q:** We're running nbb-logseq from graph-parser which has better-sqlite3. Why do we need it locally?

**A:** Node.js resolves native modules from the **script's location**, not the **execution directory**.

## The Flow

### What Happens When Scripts Run:

```
1. User runs:
   ./run-script.sh list_all_tasks.cljs mcp

2. run-script.sh does:
   cd /Users/slim/slimslenderslacks/logseq/deps/graph-parser
   ./node_modules/.bin/nbb-logseq \
     -cp "..." \
     /Users/slim/slimslenderslacks/mcp-logseq/scripts/list_all_tasks.cljs

3. Script executes from:
   Location: /Users/slim/slimslenderslacks/mcp-logseq/scripts/list_all_tasks.cljs

4. Script requires:
   (require '[logseq.db.common.sqlite-cli :as sqlite-cli])

5. sqlite-cli requires:
   (require ["better-sqlite3" :as sqlite3])  ; Line 3 of sqlite_cli.cljs

6. Node.js searches from SCRIPT location:
   /Users/slim/slimslenderslacks/mcp-logseq/scripts/node_modules/  ❌
   /Users/slim/slimslenderslacks/mcp-logseq/node_modules/          ✓ FOUND!
   /Users/slim/slimslenderslacks/node_modules/                     (not needed)
   ...
```

## Why `cd graph-parser` Doesn't Help

```bash
# run-script.sh does this:
cd "$GRAPH_PARSER_DIR"
./node_modules/.bin/nbb-logseq -cp "$CLASSPATH" \
    "$SCRIPT_DIR/scripts/$SCRIPT_NAME" "$@"
    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    Absolute path to mcp-logseq/scripts/
```

**Node.js Module Resolution:**
- Current Working Directory (CWD): `/path/to/graph-parser` ← from `cd`
- Script File Location: `/path/to/mcp-logseq/scripts/list_all_tasks.cljs`
- Module Resolution: **Uses script file location, NOT CWD**

## Verification

### Test 1: Without Local better-sqlite3

```bash
rm -rf node_modules
./run-script.sh list_all_tasks.cljs mcp

# Result:
Error: Cannot find module 'better-sqlite3'
Require stack:
- /Users/slim/slimslenderslacks/mcp-logseq/scripts/list_all_tasks.cljs
```

**Why it failed:**
- Script location: `mcp-logseq/scripts/`
- Node.js searched: `mcp-logseq/node_modules/` ❌ Not found
- Graph-parser has it: `graph-parser/node_modules/` ✓ But not in search path

### Test 2: With Symlink

```bash
mkdir -p node_modules
ln -s /path/to/graph-parser/node_modules/better-sqlite3 \
      node_modules/better-sqlite3

./run-script.sh list_all_tasks.cljs mcp

# Result: ✓ Works!
```

**Why it works:**
- Node.js searches: `mcp-logseq/node_modules/better-sqlite3`
- Finds symlink pointing to: `graph-parser/node_modules/better-sqlite3`
- Follows symlink and loads native module

## The Require Chain

```
Our Script (list_all_tasks.cljs)
        ↓ requires
logseq.db.common.sqlite-cli  ← From classpath (Logseq source)
        ↓ requires (line 3)
"better-sqlite3"             ← Node.js native module
        ↓ searches from
Script file location         ← /mcp-logseq/scripts/
        ↓ finds in
../node_modules/             ← /mcp-logseq/node_modules/
        ↓ which is
Symlink to graph-parser      ← Actual module location
```

## Alternative Solutions Considered

### ❌ Option 1: npm install better-sqlite3
```bash
npm install better-sqlite3
```
**Problems:**
- Duplicates 41 packages (38MB)
- Two copies of same module
- Version sync issues
- Unnecessary disk usage

### ❌ Option 2: Copy scripts to graph-parser
```bash
cp scripts/* /path/to/graph-parser/scripts/
cd /path/to/graph-parser
./node_modules/.bin/nbb-logseq scripts/list_all_tasks.cljs
```
**Problems:**
- Pollutes Logseq installation
- Can't keep scripts in own repo
- Harder to maintain

### ❌ Option 3: Set NODE_PATH
```bash
export NODE_PATH=/path/to/graph-parser/node_modules
./run-script.sh list_all_tasks.cljs
```
**Problems:**
- Global environment variable
- Fragile (easy to forget)
- Doesn't work consistently with native modules

### ✓ Option 4: npm install (Our Solution)
```bash
npm install better-sqlite3
```
**Benefits:**
- ✓ Standard Node.js approach
- ✓ Simple and familiar
- ✓ Works with native modules
- ✓ Keeps projects independent
- ✓ Easy to maintain

## Why Not Other Dependencies?

### @logseq/nbb-logseq - Don't Need Locally
```bash
# We explicitly use graph-parser's version
/path/to/graph-parser/node_modules/.bin/nbb-logseq
```
**Reason:** We call it with absolute path in run-script.sh

### datascript - Don't Need Locally
```clojure
(require '[datascript.core :as d])  ; Built into nbb-logseq
```
**Reason:** Pre-bundled inside nbb-logseq runtime

### Logseq Source - Don't Need Locally
```bash
CLASSPATH="$CLASSPATH:$LOGSEQ/deps/db/src"
```
**Reason:** Added to classpath, nbb-logseq finds it there

### better-sqlite3 - NEED Locally (Symlinked)
```clojure
(:require ["better-sqlite3" :as sqlite3])  ; From sqlite_cli.cljs
```
**Reason:**
- Native Node.js module (`.node` binary)
- Node.js requires it relative to script location
- Must be in `mcp-logseq/node_modules/`
- Symlink avoids duplication

## Summary

**Where it's used:**
```clojure
;; In Logseq's code: deps/db/src/logseq/db/common/sqlite_cli.cljs
(ns logseq.db.common.sqlite-cli
  (:require ["better-sqlite3" :as sqlite3]  ; ← Line 3
            ...))
```

**Why we need it:**
- Our scripts require `logseq.db.common.sqlite-cli`
- That code requires `better-sqlite3`
- Node.js resolves it from our script's location
- Must exist at: `mcp-logseq/node_modules/better-sqlite3`

**Why npm install is best:**
- Standard approach (familiar to all Node.js developers)
- Simple setup (just `npm install`)
- Works with native modules (vs NODE_PATH)
- Clean separation (vs copying scripts)

**Setup:**
```bash
npm install  # Installs better-sqlite3 locally
```
