#!/bin/bash
# Wrapper script to run nbb-logseq scripts with Logseq source in classpath

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SCRIPT_NAME="$1"
shift  # Remove script name from arguments

if [ -z "$SCRIPT_NAME" ]; then
    echo "Usage: ./run-script.sh <script-name> [args...]"
    echo ""
    echo "Available scripts:"
    ls -1 "$SCRIPT_DIR/scripts"/*.cljs | xargs -n1 basename
    exit 1
fi

# Use Logseq's nbb-logseq installation (has required feat-db-v31 features)
# Detect if running in Docker or locally
if [ -d "/app/logseq" ]; then
    # Docker environment
    LOGSEQ="/app/logseq"
else
    # Local environment
    LOGSEQ="/Users/slim/slimslenderslacks/logseq"
fi

LOGSEQ_DEPS="$LOGSEQ/deps"
GRAPH_PARSER_DIR="$LOGSEQ_DEPS/graph-parser"

# Build classpath: local scripts + Logseq source
CLASSPATH="$SCRIPT_DIR/scripts"
CLASSPATH="$CLASSPATH:$LOGSEQ/src"
CLASSPATH="$CLASSPATH:$LOGSEQ_DEPS/cli/src"
CLASSPATH="$CLASSPATH:$LOGSEQ_DEPS/common/src"
CLASSPATH="$CLASSPATH:$LOGSEQ_DEPS/db/src"
CLASSPATH="$CLASSPATH:$LOGSEQ_DEPS/graph-parser/src"
CLASSPATH="$CLASSPATH:$LOGSEQ_DEPS/outliner/src"

# Run using Logseq's nbb-logseq (has feat-db-v31 features)
cd "$GRAPH_PARSER_DIR"
./node_modules/.bin/nbb-logseq -cp "$CLASSPATH" \
    "$SCRIPT_DIR/scripts/$SCRIPT_NAME" "$@"
