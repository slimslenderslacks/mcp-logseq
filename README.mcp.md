# MCP Logseq Server

A Model Context Protocol (MCP) server that provides programmatic access to Logseq graphs via tools and resources.

## Features

### Tools
- **Database Query Tools**: Query Logseq SQLite databases
  - `list_all_tasks` - List all tasks in a graph
  - `list_tasks_by_status` - Group tasks by status
  - `list_done_tasks` - List completed tasks
  - `find_tasks` - Find tasks by criteria
  - `list_journals` - List journal pages
  - `list_pages` - List all pages in a graph
  - `get_page` - Get a page's content including its blocks
  - `list_tags` - List all tags in a graph
  - `list_properties` - List all properties in a graph

- **API Tools**: Modify Logseq via HTTP API (requires Logseq running)
  - `create_task` - Create new tasks
  - `complete_task` - Mark tasks as done
  - `update_task_status` - Change task status
  - `get_task_info` - Get task details

### Resources
- Tasks are exposed as resources with URIs: `logseq://{graph}/task/{uuid}`
- Resources are automatically updated when tasks are created or modified
- List resources with `listResources` on URI pattern `logseq://tasks/{graph}`

## Prerequisites

**For API Tools:**
1. Logseq must be running on the host
2. HTTP API must be enabled:
   - Open Logseq
   - Go to Settings → Features → Developer Mode
   - Enable "HTTP APIs server"
3. The API should be accessible at `http://localhost:12315/api`

**For Database Query Tools:**
- Logseq graphs must be in `~/logseq/graphs/`
- No Logseq instance needs to be running

## Using the Docker Image

### Pull from Docker Hub

The image is available for both **amd64** (Intel/AMD) and **arm64** (Apple Silicon, AWS Graviton) architectures:

```bash
docker pull slimslenderslacks/mcp-logseq:latest
```

Docker automatically pulls the correct architecture for your system.

### Or Build Locally

**Single platform (current architecture):**
```bash
docker build -f Dockerfile.mcp -t slimslenderslacks/mcp-logseq:latest .
```

**Multi-platform (amd64 + arm64):**
```bash
./build-multiplatform.sh
```

See [Multi-Platform Build Guide](docs/MULTIPLATFORM_BUILD.md) for details.

## Running the MCP Server

### With Docker

Mount your Logseq graphs directory:

```bash
docker run --rm \
  --add-host=host.docker.internal:host-gateway \
  -v "$HOME/logseq/graphs:/root/logseq/graphs" \
  slimslenderslacks/mcp-logseq:latest
```

### With Claude Desktop

Add to your `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "logseq": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-i",
        "--add-host=host.docker.internal:host-gateway",
        "-v",
        "/Users/yourusername/logseq/graphs:/root/logseq/graphs",
        "slimslenderslacks/mcp-logseq:latest"
      ]
    }
  }
}
```

Replace `/Users/yourusername` with your actual home directory.

### With Any MCP Client

The server uses stdio transport and follows the MCP protocol specification. Connect via:

```bash
docker run --rm -i \
  --add-host=host.docker.internal:host-gateway \
  -v "$HOME/logseq/graphs:/root/logseq/graphs" \
  slimslenderslacks/mcp-logseq:latest
```

## Usage Examples

### Via MCP Client

```javascript
// List all tasks
await client.callTool("list_all_tasks", { graph: "mcp" });

// Create a task
await client.callTool("create_task", {
  page: "Feb 7th, 2026",
  content: "Review MCP documentation",
  status: "Todo",
  priority: "High"
});

// Complete a task
await client.callTool("complete_task", {
  uuid: "6985211f-77e3-4cf0-b768-cc49f415a2b6"
});

// List all pages
await client.callTool("list_pages", {
  graph: "mcp"
});

// List pages with expanded details (includes timestamps)
await client.callTool("list_pages", {
  graph: "mcp",
  expand: true
});

// Get a specific page by name
await client.callTool("get_page", {
  graph: "mcp",
  pageName: "project-ideas"
});

// Get a page by UUID
await client.callTool("get_page", {
  graph: "mcp",
  pageName: "6985211f-77e3-4cf0-b768-cc49f415a2b6"
});

// List all tags
await client.callTool("list_tags", {
  graph: "mcp"
});

// List tags with expanded details (parents, properties)
await client.callTool("list_tags", {
  graph: "mcp",
  expand: true
});

// List all properties
await client.callTool("list_properties", {
  graph: "mcp"
});

// List properties with expanded details (type, cardinality)
await client.callTool("list_properties", {
  graph: "mcp",
  expand: true
});

// List resources (tasks)
await client.listResources("logseq://tasks/mcp");
```

## Tool Descriptions

### list_all_tasks
**Parameters:**
- `graph` (required): The name of the Logseq graph (e.g., "mcp", "Demo")

**Returns:** List of all tasks with ID, UUID, title, status, and priority

### create_task
**Parameters:**
- `page` (required): The page name or date (e.g., "Feb 7th, 2026")
- `content` (required): The task content/title
- `status` (optional): Todo, Doing, Done, Later, Now, Waiting, Canceled (default: Todo)
- `priority` (optional): High, Medium, Low (default: Medium)

**Returns:** Created task UUID

**Requires:** Logseq running with HTTP API enabled

### complete_task
**Parameters:**
- `uuid` (required): The UUID of the task block

**Returns:** Success confirmation

**Requires:** Logseq running with HTTP API enabled

### list_pages
**Parameters:**
- `graph` (required): The name of the Logseq graph (e.g., "mcp", "Demo")
- `expand` (optional): Boolean to provide additional detail on each page (includes created-at and updated-at)

**Returns:** List of all pages with title and UUID. If expand is true, also includes created-at and updated-at timestamps.

### get_page
**Parameters:**
- `graph` (required): The name of the Logseq graph (e.g., "mcp", "Demo")
- `pageName` (required): The page's name or UUID to retrieve

**Returns:** Page information including title, name, UUID, timestamps, and all blocks on the page with their content and properties. Note: A property and a tag are pages in Logseq.

### list_tags
**Parameters:**
- `graph` (required): The name of the Logseq graph (e.g., "mcp", "Demo")
- `expand` (optional): Boolean to provide additional detail on each tag (e.g. their parents (extends) and tag properties)

**Returns:** List of all tags with title and UUID. If expand is true, also includes parent classes (extends), tag properties, view type, description, icon, and timestamps.

### list_properties
**Parameters:**
- `graph` (required): The name of the Logseq graph (e.g., "mcp", "Demo")
- `expand` (optional): Boolean to provide additional detail on each property (e.g. property type, cardinality)

**Returns:** List of all properties with title and UUID. If expand is true, also includes property type, classes, schema, cardinality, description, public visibility, closed values, and timestamps.

## Architecture

```
MCP Client (Claude, etc.)
    ↓ stdio (JSON-RPC)
MCP Logseq Server (Go)
    ↓ executes
ClojureScript Scripts (nbb-logseq)
    ↓ queries/modifies
Logseq Database (SQLite) or HTTP API
```

## Error Handling

The server provides helpful error messages when:
- Logseq API is not available
- Graph doesn't exist
- Invalid parameters are provided
- Script execution fails

Example error message:
```
API script execution failed: fetch failed

Logseq API appears to be unavailable. Please ensure:
  1. Logseq is running on your host
  2. HTTP API is enabled (Settings > Features > Developer Mode > HTTP APIs)
  3. The container can reach host.docker.internal:12315
```

## Resource URIs

Tasks are exposed as resources with the following URI format:
```
logseq://{graph}/task/{uuid}
```

Example:
```
logseq://mcp/task/6985211f-77e3-4cf0-b768-cc49f415a2b6
```

List all task resources for a graph:
```
logseq://tasks/mcp
```

## Development

### Building Locally

```bash
# Install dependencies
go mod download
npm install

# Build Go server
go build -o mcp-logseq-server .

# Run locally (with local Logseq installation)
./mcp-logseq-server
```

### Testing

```bash
# Test with MCP Inspector
npx @modelcontextprotocol/inspector docker run --rm -i \
  --add-host=host.docker.internal:host-gateway \
  -v "$HOME/logseq/graphs:/root/logseq/graphs" \
  slimslenderslacks/mcp-logseq:latest
```

## Troubleshooting

### "Logseq API not available"
1. Start Logseq
2. Enable HTTP APIs: Settings → Features → Developer Mode → HTTP APIs server
3. Verify API responds: `curl http://localhost:12315/api`

### "Cannot find database"
1. Check graph exists: `ls ~/logseq/graphs/`
2. Verify volume mount in docker run command
3. Ensure graph name is correct

### "Script execution failed"
1. Check Docker logs for detailed error messages
2. Verify run-script.sh is executable
3. Ensure all dependencies are installed in the image

## License

MIT
