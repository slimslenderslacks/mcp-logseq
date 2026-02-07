# Fractional Indexing Dependency

## What is it?

`clj_fractional_indexing.cljc` is a ClojureScript port of the [fractional-indexing](https://github.com/rocicorp/fractional-indexing) JavaScript library, which provides ordering for items in a list that can be efficiently reordered.

## Why do we need it?

The file is **required by the Logseq database code**, specifically by `logseq.db.common.order`, which provides ordering functionality for:
- Blocks (ordering children within a parent)
- Properties (ordering in property lists)
- Closed values (ordering enum values)
- Other ordered sequences in Logseq

## Dependency Chain

```
Our Scripts (list_all_tasks.cljs, etc.)
    ↓ requires
logseq.db.common.sqlite-cli
    ↓ requires
logseq.db.common.sqlite
logseq.db.sqlite.util (via nbb cache)
    ↓ requires (indirectly through nbb bundling)
logseq.db.common.order
    ↓ requires
logseq.clj-fractional-indexing  ← The file we need!
```

## The Problem

The `clj_fractional_indexing.cljc` file exists in your local Logseq installation at:
```
/Users/slim/slimslenderslacks/logseq/src/logseq/clj_fractional_indexing.cljc
```

However, it is **untracked** (not committed to the Logseq GitHub repository):
```bash
$ git status src/logseq/clj_fractional_indexing.cljc
Untracked files:
  (use "git add <file>..." to include in what will be committed)
	src/logseq/clj_fractional_indexing.cljc
```

This means when we clone Logseq from GitHub in Docker, this file doesn't exist, causing:
```
Error: Could not find namespace: logseq.clj-fractional-indexing
```

## Why is it untracked?

The file was likely generated or added manually in your local Logseq development environment. The official Logseq repository may:
1. Generate this file during build
2. Have it in a different location
3. Use a different version/approach
4. Not have published it yet to the master branch

## Our Solution

### For Docker
We copy the file from your local Logseq installation into the Docker image during build:

**In mcp-logseq project:**
```bash
deps/logseq/clj_fractional_indexing.cljc  # Copied from local Logseq
```

**In Dockerfile:**
```dockerfile
# Copy missing fractional-indexing file to logseq (not in official GitHub repo)
RUN mkdir -p /app/logseq/src/logseq && \
    cp ./deps/logseq/clj_fractional_indexing.cljc /app/logseq/src/logseq/
```

### For Local Development
Your local scripts work because the file already exists in your Logseq installation's src directory, which is added to the classpath.

## When is it actually used?

Even though our scripts only **read** the database, the namespace gets loaded because:

1. **nbb-logseq** pre-compiles and bundles dependencies
2. The sqlite utilities are part of this bundle
3. They transitively require the order namespace
4. Which requires fractional-indexing

The namespace is loaded at **startup time** when requiring database code, not at runtime when querying.

## Testing

To verify it's needed:
```bash
# Temporarily hide the file
mv ~/.../logseq/src/logseq/clj_fractional_indexing.cljc{,.bak}

# Try to run a script
./run-script.sh list_all_tasks.cljs mcp
# Result: Error: Could not find namespace: logseq.clj-fractional-indexing

# Restore it
mv ~/.../logseq/src/logseq/clj_fractional_indexing.cljc{.bak,}
```

## File Contents

The file is a ~260 line ClojureScript port that implements:
- Base-62 digit encoding
- Key generation between two positions
- Ordering algorithms for efficient list reordering

From the file header:
```clojure
(ns logseq.clj-fractional-indexing
  "Fractional indexing to create an ordering that can be used for
   Realtime Editing of Ordered Sequences")

;; Original code from https://github.com/rocicorp/fractional-indexing,
;; It's converted to cljs by using AI.
```

## Alternatives Considered

1. **Skip it**: Doesn't work - namespace is required at startup
2. **Use NODE_PATH**: Doesn't help - file doesn't exist in official repo
3. **Install npm package**: No ClojureScript version available
4. **Copy to project** ✅ : Works, maintains single source of truth from your local install

## Future

This file should eventually be:
1. Committed to the official Logseq repository, OR
2. Generated during Logseq's build process, OR
3. Made available as a separate library

Until then, we maintain a copy in our project for Docker builds.
