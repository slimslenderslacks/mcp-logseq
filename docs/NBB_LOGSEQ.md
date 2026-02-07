# What is nbb-logseq?

## Overview

**nbb-logseq** is a specialized ClojureScript runtime for Node.js, created and maintained by Logseq.

- **Package:** `@logseq/nbb-logseq`
- **Version:** `1.2.173-feat-db-v31` (our installation)
- **Description:** "Nbb with additional libraries like datascript included"
- **Purpose:** Run ClojureScript scripts in Node.js with Logseq-specific dependencies

## What is nbb?

**nbb** (Node.js Babashka) is a ClojureScript scripting runtime built on:
- **Node.js** - JavaScript runtime
- **sci** (Small Clojure Interpreter) - Fast ClojureScript interpreter
- **ClojureScript** - Clojure that compiles to JavaScript

**Key Features:**
- Fast startup time (unlike traditional ClojureScript compilation)
- Run ClojureScript scripts directly (like Python or Ruby)
- Access to npm packages
- REPL support

## nbb vs nbb-logseq

### Standard nbb
```clojure
#!/usr/bin/env nbb
(require '[clojure.string :as str])
(println (str/upper-case "hello"))
```

### nbb-logseq (Logseq's Version)
```clojure
#!/usr/bin/env nbb
(require '[datascript.core :as d])           ; ✓ Built-in
(require '[logseq.db.common.sqlite-cli])     ; ✓ Built-in
(require '[clojure.string :as str])          ; ✓ Built-in
```

**Differences:**
- ✓ **DataScript** pre-included for database queries
- ✓ **Logseq namespaces** accessible without compilation
- ✓ **SQLite bindings** for database access
- ✓ Optimized for Logseq's use cases

## Why We Use It

### 1. No Compilation Required
```bash
# Traditional ClojureScript (slow)
clojure -M:cljs:compile script.cljs  # Takes seconds
node script.js

# nbb-logseq (fast)
nbb-logseq script.cljs  # Instant
```

### 2. Scripting-Friendly
```bash
# Works like any scripting language
./my_script.cljs arg1 arg2

# Can be used in pipes
echo "data" | nbb-logseq process.cljs | nbb-logseq analyze.cljs
```

### 3. Access to Logseq Internals
```clojure
;; Query Logseq's database directly
(require '[logseq.db.common.sqlite-cli :as sqlite-cli])
(require '[datascript.core :as d])

(def conn (sqlite-cli/open-db! "~/logseq/graphs/mcp/db.sqlite"))
(d/q '[:find ?title :where [?b :block/title ?title]] @conn)
```

### 4. npm Package Integration
```clojure
;; Use Node.js packages
(def fs (js/require "fs"))
(def path (js/require "path"))
```

## How It Works

### Execution Flow
```
1. nbb-logseq script.cljs
         ↓
2. sci (interpreter) loads script
         ↓
3. Resolves requires:
   - Built-in: datascript, logseq namespaces
   - Classpath: -cp src:deps
   - npm: via js/require
         ↓
4. Interprets ClojureScript (no compilation!)
         ↓
5. Executes in Node.js
```

### Classpath Resolution
```bash
nbb-logseq -cp src:deps:other script.cljs
```

Searches for namespaces in:
1. Built-in libraries (DataScript, etc.)
2. `src/` directory
3. `deps/` directory
4. `other/` directory

## Our Usage Pattern

### run-script.sh Wrapper
```bash
# Add all Logseq source to classpath
CLASSPATH="script"
CLASSPATH="$CLASSPATH:/path/to/logseq/src"
CLASSPATH="$CLASSPATH:/path/to/logseq/deps/db/src"
CLASSPATH="$CLASSPATH:/path/to/logseq/deps/common/src"
# ... more paths

# Run script with full classpath
nbb-logseq -cp "$CLASSPATH" script.cljs
```

### Why This Works
- nbb-logseq provides runtime + DataScript
- Classpath provides Logseq source code
- Scripts can access everything Logseq uses internally

## Example Script

```clojure
#!/usr/bin/env nbb
(ns my-script
  "Example nbb-logseq script"
  (:require [datascript.core :as d]
            [logseq.db.common.sqlite-cli :as sqlite-cli]
            [clojure.string :as str]))

(defn -main [args]
  (let [graph-name (first args)
        db-path (str (.-HOME js/process.env)
                     "/logseq/graphs/"
                     graph-name
                     "/db.sqlite")
        conn (sqlite-cli/open-db! db-path)

        ;; Run DataLog query
        results (d/q '[:find ?title
                       :where
                       [?b :block/title ?title]]
                     @conn)]

    (println "Found" (count results) "blocks")
    (doseq [[title] (take 5 results)]
      (println "-" title))))

(when (= nbb/*file* (nbb/invoked-file))
  (-main *command-line-args*))
```

## Comparison Table

| Feature | Standard Node.js | Standard nbb | nbb-logseq |
|---------|-----------------|--------------|------------|
| Language | JavaScript | ClojureScript | ClojureScript |
| Startup | Fast | Fast | Fast |
| DataScript | ❌ (manual install) | ❌ (manual install) | ✓ Built-in |
| Logseq namespaces | ❌ | ❌ | ✓ Available |
| Compilation | None | None | None |
| REPL | Node REPL | ClojureScript REPL | ClojureScript REPL |
| npm packages | ✓ | ✓ | ✓ |

## Installation

### Our Setup (via Logseq)

We use Logseq's nbb-logseq installation:
```bash
# Location
/Users/slim/slimslenderslacks/logseq/deps/graph-parser/node_modules/.bin/nbb-logseq

# Version: 1.2.173-feat-db-v31 (custom build with database features)

# Our wrapper uses this automatically
./run-script.sh script.cljs
```

**Why not npm install locally?**
- Logseq uses custom build `1.2.173-feat-db-v31` (not on npm)
- Includes database features (feat-db-v31) needed for SQLite
- Standard npm version (1.2.173) lacks these features
- Missing: `datascript.storage`, SQLite bindings, etc.

### Standalone Installation (Limited)
```bash
# Install standard version (missing database features)
npm install -g @logseq/nbb-logseq

# Or in project
npm install @logseq/nbb-logseq

# Run directly (won't work with our database scripts)
nbb-logseq script.cljs
```

**Note:** Standalone installation works for basic scripts but lacks database features needed for Logseq graph queries.

## REPL Usage

```bash
# Start REPL
nbb-logseq

# With classpath
nbb-logseq -cp src:deps

# Load a namespace
user=> (require '[datascript.core :as d])
user=> (d/q '[:find ?e :where [?e :name "Alice"]] db)
```

## Common Issues

### "Cannot find namespace"
**Problem:** Namespace not in classpath
```bash
# ❌ Missing classpath
nbb-logseq script.cljs

# ✓ With classpath
nbb-logseq -cp src:deps script.cljs
```

### "Cannot find module"
**Problem:** npm package not installed
```bash
# Install missing package
npm install better-sqlite3 datascript

# Then run
nbb-logseq script.cljs
```

## Resources

- **nbb GitHub:** https://github.com/babashka/nbb
- **nbb-logseq Package:** `@logseq/nbb-logseq` on npm
- **Logseq Source:** https://github.com/logseq/logseq
- **DataScript:** https://github.com/tonsky/datascript
- **sci:** https://github.com/babashka/sci

## Summary

**nbb-logseq** is Logseq's custom ClojureScript runtime that:
- ✓ Runs ClojureScript scripts instantly (no compilation)
- ✓ Includes DataScript and Logseq dependencies
- ✓ Provides access to Logseq's internal APIs
- ✓ Enables scripting and automation for Logseq
- ✓ Works with Node.js ecosystem (npm packages)

It's the foundation that makes all our scripts possible!
