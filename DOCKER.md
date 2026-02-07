# Docker Setup for mcp-logseq

This document explains how to build and use the Docker image for mcp-logseq.

## Overview

The Docker image provides a self-contained environment with:
- Complete Logseq source code (cloned from GitHub)
- nbb-logseq runtime with database features
- All mcp-logseq scripts
- Dependencies installed (better-sqlite3)

## Building the Image

```bash
docker build -t mcp-logseq .
```

This will:
1. Clone the logseq repository into `/app/logseq`
2. Install dependencies in `logseq/deps/graph-parser` (includes nbb-logseq)
3. Copy mcp-logseq scripts
4. Install better-sqlite3

**Note:** The build may take several minutes due to cloning the full Logseq repo and installing dependencies.

## Running Scripts

### Using the Helper Script (Recommended)

```bash
# Query tasks from mcp graph
./docker-run.sh list_all_tasks.cljs mcp

# Query tasks from Demo graph
./docker-run.sh list_all_tasks.cljs Demo

# List tasks by status
./docker-run.sh list_tasks_by_status.cljs mcp

# Create a task (requires Logseq running on host)
./docker-run.sh create_task_clean.cljs "Feb 6th, 2026" "My task" "Todo" "High"
```

### Manual Docker Run

```bash
docker run --rm \
  --add-host=host.docker.internal:host-gateway \
  -v "$HOME/logseq/graphs:/root/logseq/graphs" \
  mcp-logseq \
  -c "./run-script.sh list_all_tasks.cljs mcp"
```

## How It Works

### Database Access

The Docker container needs access to your Logseq database files:

```bash
-v "$HOME/logseq/graphs:/root/logseq/graphs"
```

This mounts your local `~/logseq/graphs` directory inside the container with read-write access (SQLite requires write access for WAL files even when just reading).

### API Access

For scripts that use the Logseq HTTP API (create, update, complete tasks):

```bash
--add-host=host.docker.internal:host-gateway
```

This allows the container to reach your host's Logseq API at `host.docker.internal:12315`.

The scripts automatically detect the Docker environment and use:
- **In Docker:** `http://host.docker.internal:12315/api`
- **Locally:** `http://localhost:12315/api`

This is controlled by the `LOGSEQ_API_HOST` environment variable (defaults to `localhost`).

## Architecture

### File Structure in Container

```
/app/
├── logseq/                    # Cloned from GitHub
│   ├── src/                   # Logseq source code
│   └── deps/
│       ├── graph-parser/
│       │   └── node_modules/
│       │       ├── better-sqlite3/
│       │       └── .bin/nbb-logseq
│       ├── cli/src
│       ├── db/src
│       └── ...
└── mcp-logseq/                # This project
    ├── scripts/               # ClojureScript scripts
    ├── run-script.sh          # Script runner
    └── node_modules/
        └── better-sqlite3/    # Installed via npm
```

### Path Resolution

The `run-script.sh` script automatically detects the environment:

```bash
if [ -d "/app/logseq" ]; then
    LOGSEQ="/app/logseq"      # Docker
else
    LOGSEQ="/Users/slim/..."  # Local
fi
```

## Two Problems Solved

### 1. API Endpoint

**Problem:** Docker containers can't access `localhost:12315` on the host.

**Solution:**
- Scripts check `LOGSEQ_API_HOST` environment variable
- Docker sets it to `host.docker.internal` via `--add-host`
- Allows container to reach host's Logseq API

### 2. Database Files

**Problem:** Database files are on the host filesystem at `~/.logseq/graphs/`.

**Solution:**
- Mount host directory as read-only volume
- Scripts access databases at same path: `/root/.logseq/graphs/<graph-name>/db.sqlite`
- Read-only mount prevents accidental modification

## Development

### Testing Local Scripts Still Work

After updating scripts for Docker compatibility:

```bash
# Should work without LOGSEQ_API_HOST (defaults to localhost)
./run-script.sh list_all_tasks.cljs mcp
```

### Testing Docker

```bash
# Build image
docker build -t mcp-logseq .

# Test database query script
./docker-run.sh list_all_tasks.cljs mcp

# Test API script (requires Logseq running on host)
./docker-run.sh create_task_clean.cljs "Feb 6th, 2026" "Test task" "Todo" "Low"
```

### Customizing API Host

You can override the API host for testing:

```bash
docker run --rm -it \
  -e LOGSEQ_API_HOST=my-host.example.com \
  -v "$HOME/.logseq/graphs:/root/.logseq/graphs:ro" \
  mcp-logseq \
  -c "./run-script.sh create_task_clean.cljs ..."
```

## Troubleshooting

### "Cannot connect to API"

**Symptoms:**
```
Error: fetch failed
```

**Solutions:**
1. Ensure Logseq is running on your host
2. Verify API server is enabled (Settings → Features → Developer Mode)
3. Check that port 12315 is accessible
4. Verify `--add-host=host.docker.internal:host-gateway` is in docker run command

### "Cannot find database"

**Symptoms:**
```
Error: unable to open database file
```

**Solutions:**
1. Verify graph exists: `ls ~/.logseq/graphs/`
2. Check volume mount: `-v "$HOME/.logseq/graphs:/root/.logseq/graphs:ro"`
3. Ensure correct graph name in command

### "Cannot find module 'better-sqlite3'"

This shouldn't happen if the image was built correctly. Rebuild the image:

```bash
docker build --no-cache -t mcp-logseq .
```

## Performance Notes

- First build is slow (~5-10 minutes) due to cloning Logseq and installing dependencies
- Subsequent builds use Docker cache and are faster
- Running scripts is fast (< 1 second for most queries)
- Database queries are read-only and safe

## Comparison: Local vs Docker

| Aspect | Local | Docker |
|--------|-------|--------|
| Setup | Run `npm install` | Build Docker image |
| Logseq Source | Requires local checkout | Cloned in image |
| Database Access | Direct file access | Via volume mount |
| API Access | `localhost:12315` | `host.docker.internal:12315` |
| Portability | Requires local Logseq | Self-contained |
| Speed | Fastest | Slight overhead |

## When to Use Docker

**Use Docker when:**
- Deploying to servers without local Logseq installation
- Running in CI/CD pipelines
- Ensuring consistent environment
- Distributing to other users

**Use Local when:**
- Developing and testing scripts
- You have Logseq checked out locally
- You want fastest execution
