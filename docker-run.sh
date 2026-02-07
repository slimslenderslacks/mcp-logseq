#!/bin/bash
# Helper script to run mcp-logseq scripts in Docker
#
# Usage:
#   ./docker-run.sh <script-name> [args...]
#
# Examples:
#   ./docker-run.sh list_all_tasks.cljs mcp
#   ./docker-run.sh create_task_clean.cljs "Feb 5th, 2026" "My task" "Todo" "High"

set -e

SCRIPT_NAME="$1"
shift || true  # Remove script name from arguments

if [ -z "$SCRIPT_NAME" ]; then
    echo "Usage: ./docker-run.sh <script-name> [args...]"
    echo ""
    echo "Available scripts:"
    docker run --rm mcp-logseq -c "ls -1 scripts/*.cljs | xargs -n1 basename"
    exit 1
fi

# Mount the logseq graphs directory from host
# Note: Mount needs write access for SQLite WAL files
# API calls will use host.docker.internal:12315
docker run --rm \
    --add-host=host.docker.internal:host-gateway \
    -v "$HOME/logseq/graphs:/root/logseq/graphs" \
    mcp-logseq \
    -c "./run-script.sh $SCRIPT_NAME $*"
