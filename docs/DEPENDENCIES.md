# Dependencies

## Summary

**We use minimal local dependencies** - only what Node.js requires for native module resolution.

## What We Use

### Local (via package.json)

**`better-sqlite3`** - SQLite3 native bindings
- **Version:** `^12.6.2`
- **Why local?** Native modules must be in local `node_modules` for Node.js to find them
- **Used by:** `logseq.db.common.sqlite-cli` (Logseq's database access layer)
- **Required:** Yes

### From Logseq Installation

**`@logseq/nbb-logseq`** - ClojureScript runtime
- **Version:** `1.2.173-feat-db-v31` (custom build)
- **Location:** `/Users/slim/slimslenderslacks/logseq/deps/graph-parser/node_modules/`
- **Why external?** Custom version not on npm, includes database features
- **Used via:** `run-script.sh` wrapper
- **Required:** Yes

**Logseq Source Code** - ClojureScript namespaces
- **Location:** `/Users/slim/slimslenderslacks/logseq/deps/*/src`
- **Includes:** `logseq.db.*`, `logseq.common.*`, etc.
- **Used via:** Classpath in `run-script.sh`
- **Required:** Yes

**DataScript** - Immutable database
- **Provided by:** nbb-logseq (built-in)
- **Location:** Part of nbb-logseq bundle
- **Required:** Yes (but don't install locally)

## Installation

```bash
cd ~/slimslenderslacks/mcp-logseq

# Install only required local dependency
npm install
```

This installs just `better-sqlite3`. Everything else comes from the Logseq installation.

## Why So Few Dependencies?

### Tested Without node_modules:
```bash
rm -rf node_modules
./run-script.sh list_all_tasks.cljs mcp
# Error: Cannot find module 'better-sqlite3'
```

### Why better-sqlite3 Must Be Local:

**Node.js Native Module Resolution:**
1. Script runs from: `/Users/slim/slimslenderslacks/mcp-logseq/scripts/`
2. Logseq code calls: `(js/require "better-sqlite3")`
3. Node.js searches from script location:
   - `./node_modules/` ✓ Found!
   - `../node_modules/` ✓ Found!
   - `/Users/slim/slimslenderslacks/logseq/.../node_modules/` ✗ Too far

**Why it needs to be local:**
- Native modules (`.node` files) are architecture-specific
- Node.js resolves them relative to the requiring script
- Even though nbb-logseq is external, the SQLite module must be local

### Why Other Things Aren't Needed:

**`datascript`** - Built into nbb-logseq
```clojure
(require '[datascript.core :as d])  ; Works without local install
```

**`@logseq/nbb-logseq`** - Use Logseq's version
```bash
# Their version: 1.2.173-feat-db-v31 (not on npm)
# npm version:   1.2.173 (missing features)
```

**Logseq namespaces** - Via classpath
```clojure
(require '[logseq.db.common.sqlite-cli])  ; From classpath
```

## Dependency Flow

```
mcp-logseq scripts
       ↓
   (requires)
       ↓
better-sqlite3 ← must be local (native module)
       ↓
   (uses via)
       ↓
logseq.db.common.sqlite-cli ← from classpath
       ↓
   (runs in)
       ↓
@logseq/nbb-logseq ← from Logseq installation
       ↓
   (includes)
       ↓
datascript.core ← built-in to nbb-logseq
```

## Troubleshooting

### "Cannot find module 'better-sqlite3'"
```bash
# Install local dependency
npm install
```

### "Could not find namespace: logseq.db.common.sqlite-cli"
```bash
# Check Logseq installation exists
ls /Users/slim/slimslenderslacks/logseq/deps/db/src/logseq/db/common/

# Check run-script.sh classpath
grep CLASSPATH run-script.sh
```

### "Cannot find module 'datascript'"
This shouldn't happen - datascript is built into nbb-logseq. If you see this:
```bash
# Check nbb-logseq version
/Users/slim/slimslenderslacks/logseq/deps/graph-parser/node_modules/.bin/nbb-logseq --version
```

## Development Setup

### First Time Setup
```bash
# 1. Clone/setup mcp-logseq
cd ~/slimslenderslacks/mcp-logseq

# 2. Install local dependencies
npm install

# 3. Verify Logseq installation exists
ls /Users/slim/slimslenderslacks/logseq/deps/graph-parser/node_modules/.bin/nbb-logseq

# 4. Test
./run-script.sh list_all_tasks.cljs mcp
```

### Adding New Dependencies

**If you need a native Node.js module:**
```bash
npm install <module-name>
```

**If you need a ClojureScript library:**
- Check if it's built into nbb-logseq (most core libs are)
- If not, add to Logseq installation (not recommended)
- Or consider alternative approach

## Comparison

### Before Cleanup:
```json
{
  "dependencies": {
    "@logseq/nbb-logseq": "^1.2.173",
    "better-sqlite3": "^12.6.2",
    "datascript": "^1.7.8"
  }
}
```
- ❌ Wrong nbb-logseq version (missing feat-db-v31)
- ❌ Duplicate datascript (built into nbb-logseq)
- ✓ Need better-sqlite3

### After Cleanup:
```json
{
  "dependencies": {
    "better-sqlite3": "^12.6.2"
  }
}
```
- ✓ Minimal dependencies
- ✓ Only what's actually needed
- ✓ Clear and maintainable

## Summary

**We need:**
- ✓ `better-sqlite3` locally (native module resolution)
- ✓ Logseq installation at `/Users/slim/slimslenderslacks/logseq/`

**We don't need:**
- ✗ Local `@logseq/nbb-logseq` (use Logseq's)
- ✗ Local `datascript` (built into nbb-logseq)
- ✗ Local Logseq source (via classpath)

This keeps our project minimal while leveraging the parent Logseq installation for runtime and libraries.
