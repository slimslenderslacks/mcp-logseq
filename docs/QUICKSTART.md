# Quick Start: Convert File Graph to DB Graph

## TL;DR

Convert your file-based Logseq graph to a DB graph in 3 steps:

```bash
# Step 1: Install dependencies
cd deps/cli && yarn install

# Step 2: Convert your graph
node cli.mjs import-file-graph --graph /path/to/your/graph

# Step 3: Open in Logseq!
# The DB graph will appear in your graph list
```

## Your Existing Graphs

You have these file-based graphs that can be converted:

```bash
# Convert cooking graph
node deps/cli/cli.mjs import-file-graph --graph /Users/slim/graphs/cooking

# Convert journal graph
node deps/cli/cli.mjs import-file-graph --graph /Users/slim/graphs/journal

# Convert mcp graph
node deps/cli/cli.mjs import-file-graph --graph /Users/slim/graphs/mcp
```

## Using nbb Directly (No Build Required)

If you have nbb installed, you can use the standalone script:

```bash
# Direct usage
nbb scripts/file-graph-to-db.cljs /Users/slim/graphs/cooking

# With options
nbb scripts/file-graph-to-db.cljs /Users/slim/graphs/cooking -v -V
```

## Common Use Cases

### Convert with validation

```bash
node deps/cli/cli.mjs import-file-graph \
  --graph ~/graphs/my-notes \
  --validate \
  --verbose
```

### Convert and convert all tags to classes

```bash
node deps/cli/cli.mjs import-file-graph \
  --graph ~/graphs/my-notes \
  --all-tags
```

### Overwrite existing DB graph

```bash
node deps/cli/cli.mjs import-file-graph \
  --graph ~/graphs/my-notes \
  --force
```

### Custom DB graph name

```bash
node deps/cli/cli.mjs import-file-graph \
  --graph ~/graphs/my-notes \
  --name "my-awesome-graph"
```

## Verify Conversion

After converting, verify the DB was created:

```bash
# List all graphs
node deps/cli/cli.mjs list

# Show graph info
node deps/cli/cli.mjs show logseq_db_cooking

# Validate the graph
node deps/cli/cli.mjs validate --graphs logseq_db_cooking

# Open Logseq app and the graph should appear in your list!
```

## Troubleshooting

### "Cannot find package '@logseq/nbb-logseq'"

Install dependencies first:
```bash
cd deps/cli
yarn install
```

### "No 'logseq/config.edn' found"

Make sure your directory is a valid Logseq graph with a `logseq/config.edn` file.

### "DB graph already exists"

Use `--force` to overwrite:
```bash
node deps/cli/cli.mjs import-file-graph --graph ~/graphs/my-notes --force
```

## What Happens During Conversion

1. ✅ Scans all markdown files in your graph
2. ✅ Parses blocks, pages, properties, tags
3. ✅ Copies assets (images, PDFs, etc.)
4. ✅ Creates SQLite database at `~/.logseq/graphs/logseq_db_<name>/`
5. ✅ Ready to use in Logseq app!

## Need More Help?

- Full documentation: See `FILE_GRAPH_TO_DB.md`
- Implementation details: See `IMPLEMENTATION_SUMMARY.md`
- Get help: `node deps/cli/cli.mjs help import-file-graph`
