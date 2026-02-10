package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/exec"
	"strings"
	"sync"

	"github.com/modelcontextprotocol/go-sdk/mcp"
)

// Task represents a Logseq task
type Task struct {
	ID       int    `json:"id"`
	UUID     string `json:"uuid"`
	Title    string `json:"title"`
	Status   string `json:"status"`
	Priority string `json:"priority"`
}

// MCPServer holds the MCP server and task cache
type MCPServer struct {
	server *mcp.Server
	tasks  map[string][]Task // graph -> tasks
	mu     sync.RWMutex
}

func main() {
	if err := run(); err != nil {
		log.Fatal(err)
	}
}

func run() error {
	// Check if Logseq API is available
	if err := checkLogseqAPI(); err != nil {
		apiHost := os.Getenv("LOGSEQ_API_HOST")
		if apiHost == "" {
			apiHost = "host.docker.internal"
		}
		apiPort := os.Getenv("LOGSEQ_API_PORT")
		if apiPort == "" {
			apiPort = "12315"
		}
		log.Printf("⚠ WARNING: Logseq API not available: %v", err)
		log.Println("⚠ Some features (create_task, complete_task, update_task_status) will not work")
		log.Println("⚠ To enable API features:")
		log.Println("  1. Start Logseq on your host")
		log.Println("  2. Enable HTTP API: Settings > Features > Developer Mode > HTTP APIs")
		log.Printf("  3. Ensure the API is accessible at %s:%s\n", apiHost, apiPort)
	} else {
		log.Println("✓ Logseq API is accessible")
	}

	log.Println("Starting MCP Logseq Server...")
	log.Println("This server provides programmatic access to Logseq via SQLite queries and HTTP API")

	// Create server
	server := mcp.NewServer(&mcp.Implementation{
		Name:    "mcp-logseq",
		Version: "1.0.0",
	}, nil)

	mcpServer := &MCPServer{
		server: server,
		tasks:  make(map[string][]Task),
	}

	// Register tools and resources
	registerTools(mcpServer)
	registerResources(mcpServer)

	// Run server with stdio transport
	log.Println("Ready to accept MCP protocol messages on stdio")
	transport := &mcp.StdioTransport{}
	return server.Run(context.Background(), transport)
}

func checkLogseqAPI() error {
	apiHost := os.Getenv("LOGSEQ_API_HOST")
	if apiHost == "" {
		apiHost = "host.docker.internal"
	}

	apiPort := os.Getenv("LOGSEQ_API_PORT")
	if apiPort == "" {
		apiPort = "12315"
	}

	resp, err := http.Get(fmt.Sprintf("http://%s:%s/api", apiHost, apiPort))
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	if resp.StatusCode >= 400 {
		return fmt.Errorf("API returned status %d", resp.StatusCode)
	}
	return nil
}

type ListAllTasksArgs struct {
	Graph string `json:"graph"`
}

type FindTasksArgs struct {
	Graph string `json:"graph"`
}

type ListTasksByStatusArgs struct {
	Graph string `json:"graph"`
}

type CreateTaskArgs struct {
	Page     string `json:"page"`
	Content  string `json:"content"`
	Status   string `json:"status"`
	Priority string `json:"priority"`
}

type CompleteTaskArgs struct {
	UUID string `json:"uuid"`
}

type UpdateTaskStatusArgs struct {
	UUID   string `json:"uuid"`
	Status string `json:"status"`
}

type ListPagesArgs struct {
	Graph  string `json:"graph"`
	Expand bool   `json:"expand"`
}

type GetPageArgs struct {
	Graph    string `json:"graph"`
	PageName string `json:"pageName"`
}

type ListTagsArgs struct {
	Graph  string `json:"graph"`
	Expand bool   `json:"expand"`
}

type ListPropertiesArgs struct {
	Graph  string `json:"graph"`
	Expand bool   `json:"expand"`
}

func registerTools(mcpServer *MCPServer) {
	// Database Query Tools
	mcp.AddTool(
		mcpServer.server,
		&mcp.Tool{
			Name:        "list_all_tasks",
			Description: "List all tasks from a Logseq graph database. Returns task ID, UUID, title, status, and priority.",
			InputSchema: map[string]any{
				"type": "object",
				"properties": map[string]any{
					"graph": map[string]any{
						"type":        "string",
						"description": "The name of the Logseq graph (e.g., 'mcp', 'Demo')",
					},
				},
				"required": []string{"graph"},
			},
		},
		func(ctx context.Context, request *mcp.CallToolRequest, args ListAllTasksArgs) (*mcp.CallToolResult, any, error) {
			return mcpServer.executeScript(ctx, "list_all_tasks.cljs", map[string]any{
				"graph": args.Graph,
			})
		},
	)

	mcp.AddTool(
		mcpServer.server,
		&mcp.Tool{
			Name:        "list_tasks_by_status",
			Description: "List tasks grouped by status (Todo, Doing, Done, Backlog) from a Logseq graph.",
			InputSchema: map[string]any{
				"type": "object",
				"properties": map[string]any{
					"graph": map[string]any{
						"type":        "string",
						"description": "The name of the Logseq graph (e.g., 'mcp', 'Demo')",
					},
				},
				"required": []string{"graph"},
			},
		},
		func(ctx context.Context, request *mcp.CallToolRequest, args ListTasksByStatusArgs) (*mcp.CallToolResult, any, error) {
			return mcpServer.executeScript(ctx, "list_tasks_by_status.cljs", map[string]any{
				"graph": args.Graph,
			})
		},
	)

	mcp.AddTool(
		mcpServer.server,
		&mcp.Tool{
			Name:        "find_tasks",
			Description: "Find tasks matching specific criteria in a Logseq graph.",
			InputSchema: map[string]any{
				"type": "object",
				"properties": map[string]any{
					"graph": map[string]any{
						"type":        "string",
						"description": "The name of the Logseq graph (e.g., 'mcp', 'Demo')",
					},
				},
				"required": []string{"graph"},
			},
		},
		func(ctx context.Context, request *mcp.CallToolRequest, args FindTasksArgs) (*mcp.CallToolResult, any, error) {
			return mcpServer.executeScript(ctx, "find_tasks.cljs", map[string]any{
				"graph": args.Graph,
			})
		},
	)

	mcp.AddTool(
		mcpServer.server,
		&mcp.Tool{
			Name:        "list_pages",
			Description: "List all pages in a graph",
			InputSchema: map[string]any{
				"type": "object",
				"properties": map[string]any{
					"graph": map[string]any{
						"type":        "string",
						"description": "The name of the Logseq graph (e.g., 'mcp', 'Demo')",
					},
					"expand": map[string]any{
						"type":        "boolean",
						"description": "Provide additional detail on each page (includes created-at and updated-at timestamps)",
						"default":     false,
					},
				},
				"required": []string{"graph"},
			},
		},
		func(ctx context.Context, request *mcp.CallToolRequest, args ListPagesArgs) (*mcp.CallToolResult, any, error) {
			expandStr := "false"
			if args.Expand {
				expandStr = "true"
			}
			return mcpServer.executeScriptWithArgs(ctx, "list_pages.cljs", args.Graph, []string{expandStr})
		},
	)

	mcp.AddTool(
		mcpServer.server,
		&mcp.Tool{
			Name:        "get_page",
			Description: "Get a page's content including its blocks. A property and a tag are pages.",
			InputSchema: map[string]any{
				"type": "object",
				"properties": map[string]any{
					"graph": map[string]any{
						"type":        "string",
						"description": "The name of the Logseq graph (e.g., 'mcp', 'Demo')",
					},
					"pageName": map[string]any{
						"type":        "string",
						"description": "The page's name or UUID to retrieve. A property and a tag are pages.",
					},
				},
				"required": []string{"graph", "pageName"},
			},
		},
		func(ctx context.Context, request *mcp.CallToolRequest, args GetPageArgs) (*mcp.CallToolResult, any, error) {
			return mcpServer.executeScriptWithArgs(ctx, "get_page.cljs", args.Graph, []string{args.PageName})
		},
	)

	mcp.AddTool(
		mcpServer.server,
		&mcp.Tool{
			Name:        "list_tags",
			Description: "List all tags in a graph",
			InputSchema: map[string]any{
				"type": "object",
				"properties": map[string]any{
					"graph": map[string]any{
						"type":        "string",
						"description": "The name of the Logseq graph (e.g., 'mcp', 'Demo')",
					},
					"expand": map[string]any{
						"type":        "boolean",
						"description": "Provide additional detail on each tag (e.g. their parents/extends and tag properties)",
						"default":     false,
					},
				},
				"required": []string{"graph"},
			},
		},
		func(ctx context.Context, request *mcp.CallToolRequest, args ListTagsArgs) (*mcp.CallToolResult, any, error) {
			expandStr := "false"
			if args.Expand {
				expandStr = "true"
			}
			return mcpServer.executeScriptWithArgs(ctx, "list_tags.cljs", args.Graph, []string{expandStr})
		},
	)

	mcp.AddTool(
		mcpServer.server,
		&mcp.Tool{
			Name:        "list_properties",
			Description: "List all properties in a graph",
			InputSchema: map[string]any{
				"type": "object",
				"properties": map[string]any{
					"graph": map[string]any{
						"type":        "string",
						"description": "The name of the Logseq graph (e.g., 'mcp', 'Demo')",
					},
					"expand": map[string]any{
						"type":        "boolean",
						"description": "Provide additional detail on each property (e.g. property type, cardinality)",
						"default":     false,
					},
				},
				"required": []string{"graph"},
			},
		},
		func(ctx context.Context, request *mcp.CallToolRequest, args ListPropertiesArgs) (*mcp.CallToolResult, any, error) {
			expandStr := "false"
			if args.Expand {
				expandStr = "true"
			}
			return mcpServer.executeScriptWithArgs(ctx, "list_properties.cljs", args.Graph, []string{expandStr})
		},
	)

	// API-based Tools
	mcp.AddTool(
		mcpServer.server,
		&mcp.Tool{
			Name:        "create_task",
			Description: "Create a new task in Logseq via API. Requires Logseq to be running with HTTP API enabled.",
			InputSchema: map[string]any{
				"type": "object",
				"properties": map[string]any{
					"page": map[string]any{
						"type":        "string",
						"description": "The page name or date where the task should be created (e.g., 'Feb 7th, 2026' or 'Projects')",
					},
					"content": map[string]any{
						"type":        "string",
						"description": "The task content/title",
					},
					"status": map[string]any{
						"type":        "string",
						"description": "Task status: Todo, Doing, Done, Later, Now, Waiting, or Canceled",
						"enum":        []string{"Todo", "Doing", "Done", "Later", "Now", "Waiting", "Canceled"},
						"default":     "Todo",
					},
					"priority": map[string]any{
						"type":        "string",
						"description": "Task priority level",
						"enum":        []string{"High", "Medium", "Low"},
						"default":     "Medium",
					},
				},
				"required": []string{"page", "content"},
			},
		},
		func(ctx context.Context, request *mcp.CallToolRequest, args CreateTaskArgs) (*mcp.CallToolResult, any, error) {
			result, metadata, err := mcpServer.executeAPIScript(ctx, "create_task_clean.cljs", map[string]any{
				"page":     args.Page,
				"content":  args.Content,
				"status":   args.Status,
				"priority": args.Priority,
			})
			if err == nil {
				// Notify clients that resources have changed
				go mcpServer.notifyResourcesChanged(ctx)
			}
			return result, metadata, err
		},
	)

	mcp.AddTool(
		mcpServer.server,
		&mcp.Tool{
			Name:        "complete_task",
			Description: "Mark a task as complete (Done status) via API. Requires Logseq running.",
			InputSchema: map[string]any{
				"type": "object",
				"properties": map[string]any{
					"uuid": map[string]any{
						"type":        "string",
						"description": "The UUID of the task block to mark as complete",
					},
				},
				"required": []string{"uuid"},
			},
		},
		func(ctx context.Context, request *mcp.CallToolRequest, args CompleteTaskArgs) (*mcp.CallToolResult, any, error) {
			result, metadata, err := mcpServer.executeAPIScript(ctx, "complete_task.cljs", map[string]any{
				"uuid": args.UUID,
			})
			if err == nil {
				go mcpServer.notifyResourcesChanged(ctx)
			}
			return result, metadata, err
		},
	)

	mcp.AddTool(
		mcpServer.server,
		&mcp.Tool{
			Name:        "update_task_status",
			Description: "Update the status of a task via API. Requires Logseq running.",
			InputSchema: map[string]any{
				"type": "object",
				"properties": map[string]any{
					"uuid": map[string]any{
						"type":        "string",
						"description": "The UUID of the task block to update",
					},
					"status": map[string]any{
						"type":        "string",
						"description": "New task status",
						"enum":        []string{"Todo", "Doing", "Done", "Later", "Now", "Waiting", "Canceled"},
					},
				},
				"required": []string{"uuid", "status"},
			},
		},
		func(ctx context.Context, request *mcp.CallToolRequest, args UpdateTaskStatusArgs) (*mcp.CallToolResult, any, error) {
			result, metadata, err := mcpServer.executeAPIScript(ctx, "update_task_status.cljs", map[string]any{
				"uuid":   args.UUID,
				"status": args.Status,
			})
			if err == nil {
				go mcpServer.notifyResourcesChanged(ctx)
			}
			return result, metadata, err
		},
	)
}

func registerResources(mcpServer *MCPServer) {
	mcpServer.server.AddResource(
		&mcp.Resource{
			URI:         "logseq://tasks/*",
			Name:        "Logseq Tasks",
			Description: "All tasks in a Logseq graph. Use URI pattern: logseq://tasks/{graph}",
			MIMEType:    "application/json",
		},
		func(ctx context.Context, request *mcp.ReadResourceRequest) (*mcp.ReadResourceResult, error) {
			// Extract graph from URI
			uri := request.Params.URI
			parts := strings.Split(uri, "/")
			if len(parts) < 4 {
				return nil, fmt.Errorf("invalid URI format, expected: logseq://tasks/{graph}")
			}
			graph := parts[3]

			// Fetch tasks
			_, _, err := mcpServer.executeScript(ctx, "list_all_tasks.cljs", map[string]any{
				"graph": graph,
			})
			if err != nil {
				return nil, err
			}

			mcpServer.mu.RLock()
			defer mcpServer.mu.RUnlock()

			tasks := mcpServer.tasks[graph]
			jsonData, err := json.Marshal(tasks)
			if err != nil {
				return nil, err
			}

			return &mcp.ReadResourceResult{
				Contents: []*mcp.ResourceContents{
					{
						URI:      uri,
						MIMEType: "application/json",
						Text:     string(jsonData),
					},
				},
			}, nil
		},
	)
}

func (m *MCPServer) executeScript(ctx context.Context, scriptName string, args map[string]any) (*mcp.CallToolResult, any, error) {
	// Extract graph parameter
	graph, ok := args["graph"].(string)
	if !ok || graph == "" {
		return &mcp.CallToolResult{
			Content: []mcp.Content{
				&mcp.TextContent{Text: "Error: graph parameter is required"},
			},
			IsError: true,
		}, nil, nil
	}

	// Execute the script
	cmd := exec.CommandContext(ctx, "/app/mcp-logseq/run-script.sh", scriptName, graph)
	cmd.Env = append(os.Environ(),
		"HOME=/root",
	)

	output, err := cmd.CombinedOutput()
	if err != nil {
		return &mcp.CallToolResult{
			Content: []mcp.Content{
				&mcp.TextContent{Text: fmt.Sprintf("Script execution failed: %v\nOutput: %s", err, string(output))},
			},
			IsError: true,
		}, nil, nil
	}

	// Parse and cache tasks if this is a task listing
	if strings.Contains(scriptName, "task") {
		m.parseTasks(graph, string(output))
	}

	return &mcp.CallToolResult{
		Content: []mcp.Content{
			&mcp.TextContent{Text: string(output)},
		},
	}, nil, nil
}

func (m *MCPServer) executeScriptWithArgs(ctx context.Context, scriptName string, graph string, extraArgs []string) (*mcp.CallToolResult, any, error) {
	if graph == "" {
		return &mcp.CallToolResult{
			Content: []mcp.Content{
				&mcp.TextContent{Text: "Error: graph parameter is required"},
			},
			IsError: true,
		}, nil, nil
	}

	// Build command arguments: script name, graph, then any extra args
	cmdArgs := []string{scriptName, graph}
	cmdArgs = append(cmdArgs, extraArgs...)

	// Execute the script
	cmd := exec.CommandContext(ctx, "/app/mcp-logseq/run-script.sh", cmdArgs...)
	cmd.Env = append(os.Environ(),
		"HOME=/root",
	)

	output, err := cmd.CombinedOutput()
	if err != nil {
		return &mcp.CallToolResult{
			Content: []mcp.Content{
				&mcp.TextContent{Text: fmt.Sprintf("Script execution failed: %v\nOutput: %s", err, string(output))},
			},
			IsError: true,
		}, nil, nil
	}

	return &mcp.CallToolResult{
		Content: []mcp.Content{
			&mcp.TextContent{Text: string(output)},
		},
	}, nil, nil
}

func (m *MCPServer) executeAPIScript(ctx context.Context, scriptName string, args map[string]any) (*mcp.CallToolResult, any, error) {
	// Build arguments for the script
	var scriptArgs []string

	switch scriptName {
	case "create_task_clean.cljs":
		page, _ := args["page"].(string)
		content, _ := args["content"].(string)
		status, _ := args["status"].(string)
		priority, _ := args["priority"].(string)

		if page == "" || content == "" {
			return &mcp.CallToolResult{
				Content: []mcp.Content{
					&mcp.TextContent{Text: "Error: page and content parameters are required"},
				},
				IsError: true,
			}, nil, nil
		}
		if status == "" {
			status = "Todo"
		}
		if priority == "" {
			priority = "Medium"
		}
		scriptArgs = []string{page, content, status, priority}

	case "complete_task.cljs", "get_task_info.cljs":
		uuid, _ := args["uuid"].(string)
		if uuid == "" {
			return &mcp.CallToolResult{
				Content: []mcp.Content{
					&mcp.TextContent{Text: "Error: uuid parameter is required"},
				},
				IsError: true,
			}, nil, nil
		}
		scriptArgs = []string{uuid}

	case "update_task_status.cljs":
		uuid, _ := args["uuid"].(string)
		status, _ := args["status"].(string)
		if uuid == "" || status == "" {
			return &mcp.CallToolResult{
				Content: []mcp.Content{
					&mcp.TextContent{Text: "Error: uuid and status parameters are required"},
				},
				IsError: true,
			}, nil, nil
		}
		scriptArgs = []string{uuid, status}
	}

	// Execute the script
	cmdArgs := append([]string{scriptName}, scriptArgs...)
	cmd := exec.CommandContext(ctx, "/app/mcp-logseq/run-script.sh", cmdArgs...)

	// Pass through API configuration from environment
	envVars := []string{"HOME=/root"}
	if apiHost := os.Getenv("LOGSEQ_API_HOST"); apiHost != "" {
		envVars = append(envVars, "LOGSEQ_API_HOST="+apiHost)
	}
	if apiPort := os.Getenv("LOGSEQ_API_PORT"); apiPort != "" {
		envVars = append(envVars, "LOGSEQ_API_PORT="+apiPort)
	}
	if apiToken := os.Getenv("LOGSEQ_API_AUTHORIZATION_TOKEN"); apiToken != "" {
		envVars = append(envVars, "LOGSEQ_API_AUTHORIZATION_TOKEN="+apiToken)
	}
	cmd.Env = append(os.Environ(), envVars...)

	output, err := cmd.CombinedOutput()
	if err != nil {
		errMsg := fmt.Sprintf("API script execution failed: %v\nOutput: %s", err, string(output))
		if strings.Contains(string(output), "fetch failed") || strings.Contains(string(output), "ECONNREFUSED") {
			apiHost := os.Getenv("LOGSEQ_API_HOST")
			if apiHost == "" {
				apiHost = "host.docker.internal"
			}
			apiPort := os.Getenv("LOGSEQ_API_PORT")
			if apiPort == "" {
				apiPort = "12315"
			}
			errMsg += "\n\nLogseq API appears to be unavailable. Please ensure:\n"
			errMsg += "  1. Logseq is running on your host\n"
			errMsg += "  2. HTTP API is enabled (Settings > Features > Developer Mode > HTTP APIs)\n"
			errMsg += fmt.Sprintf("  3. The API is accessible at %s:%s", apiHost, apiPort)
		}
		return &mcp.CallToolResult{
			Content: []mcp.Content{
				&mcp.TextContent{Text: errMsg},
			},
			IsError: true,
		}, nil, nil
	}

	return &mcp.CallToolResult{
		Content: []mcp.Content{
			&mcp.TextContent{Text: string(output)},
		},
	}, nil, nil
}

func (m *MCPServer) parseTasks(graph string, output string) {
	m.mu.Lock()
	defer m.mu.Unlock()

	var tasks []Task
	lines := strings.Split(output, "\n")
	var currentTask *Task

	for _, line := range lines {
		line = strings.TrimSpace(line)
		if line == "" {
			if currentTask != nil {
				tasks = append(tasks, *currentTask)
				currentTask = nil
			}
			continue
		}

		if currentTask == nil {
			currentTask = &Task{}
		}

		if strings.HasPrefix(line, "Task ID:") {
			fmt.Sscanf(line, "Task ID: %d", &currentTask.ID)
		} else if after, ok :=strings.CutPrefix(line, "UUID:"); ok  {
			currentTask.UUID = strings.TrimSpace(after)
		} else if after, ok :=strings.CutPrefix(line, "Title:"); ok  {
			currentTask.Title = strings.TrimSpace(after)
		} else if after, ok :=strings.CutPrefix(line, "Status:"); ok  {
			currentTask.Status = strings.TrimSpace(after)
		} else if after, ok :=strings.CutPrefix(line, "Priority:"); ok  {
			currentTask.Priority = strings.TrimSpace(after)
		}
	}

	if currentTask != nil {
		tasks = append(tasks, *currentTask)
	}

	m.tasks[graph] = tasks
}

func (m *MCPServer) notifyResourcesChanged(ctx context.Context) {
	// Notify clients about all task resources that may have changed
	// This sends a notification for all graphs that have cached tasks
	m.mu.RLock()
	graphs := make([]string, 0, len(m.tasks))
	for graph := range m.tasks {
		graphs = append(graphs, graph)
	}
	m.mu.RUnlock()

	// Send resource updated notifications
	for _, graph := range graphs {
		uri := fmt.Sprintf("logseq://tasks/%s", graph)
		if err := m.server.ResourceUpdated(ctx, &mcp.ResourceUpdatedNotificationParams{
			URI: uri,
		}); err != nil {
			log.Printf("Failed to send resource update notification for %s: %v", uri, err)
		}
	}
}
