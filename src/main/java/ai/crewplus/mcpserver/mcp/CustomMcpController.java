package ai.crewplus.mcpserver.mcp;

import ai.crewplus.mcpserver.service.InstanceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Custom MCP Server.
 * 
 * Implements MCP protocol (JSON-RPC) over HTTP.
 * Supports dynamic tool discovery and execution.
 * 
 * This controller provides endpoints at both:
 * - /custom-mcp/* - Custom endpoints for testing
 * - /mcp/* - Override Spring AI MCP Server endpoints
 */
@RestController
public class CustomMcpController {

    private static final Logger log = LoggerFactory.getLogger(CustomMcpController.class);

    private final CustomMcpServer mcpServer;
    private final InstanceContext instanceContext;

    @Autowired
    public CustomMcpController(CustomMcpServer mcpServer, 
                               InstanceContext instanceContext) {
        this.mcpServer = mcpServer;
        this.instanceContext = instanceContext;
    }

    /**
     * Handle MCP initialize request.
     * Available at both /custom-mcp/initialize and /mcp/initialize
     */
    @PostMapping(value = {"/custom-mcp/initialize", "/mcp/initialize"}, 
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> initialize(
            @RequestHeader(value = "X-Toolset", required = false) String toolsetHeader,
            @RequestParam(value = "toolset", required = false) String toolsetParam,
            @RequestBody(required = false) Map<String, Object> body) {
        
        return Mono.fromCallable(() -> {
            // Extract toolset from various sources
            String toolset = toolsetHeader != null ? toolsetHeader 
                    : (toolsetParam != null ? toolsetParam 
                    : (body != null ? (String) body.get("toolset") : null));
            
            if (toolset == null || toolset.isEmpty()) {
                toolset = "default";
            }
            
            // Set toolset in context
            instanceContext.setCurrentInstance(toolset);
            
            // Initialize server with toolset
            mcpServer.initialize(toolset);
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("protocolVersion", "2025-06-18");
            
            Map<String, Object> capabilities = new HashMap<>();
            Map<String, Object> tools = new HashMap<>();
            tools.put("listChanged", false);
            capabilities.put("tools", tools);
            response.put("capabilities", capabilities);
            
            Map<String, Object> serverInfo = new HashMap<>();
            serverInfo.put("name", "custom-mcp-server");
            serverInfo.put("version", "1.0.0");
            response.put("serverInfo", serverInfo);
            
            return ResponseEntity.ok(response);
        });
    }

    /**
     * Handle MCP tools/list request.
     * Available at both /custom-mcp/tools/list and /mcp/tools/list
     */
    @PostMapping(value = {"/custom-mcp/tools/list", "/mcp/tools/list"}, 
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> listTools(
            @RequestHeader(value = "X-Toolset", required = false) String toolsetHeader,
            @RequestParam(value = "toolset", required = false) String toolsetParam) {
        
        return Mono.fromCallable(() -> {
            String toolset = toolsetHeader != null ? toolsetHeader 
                    : (toolsetParam != null ? toolsetParam 
                    : instanceContext.getCurrentInstance());
            
            if (toolset == null || toolset.isEmpty()) {
                toolset = "default";
            }
            
            List<Map<String, Object>> tools = mcpServer.listTools(toolset);
            
            Map<String, Object> response = new HashMap<>();
            response.put("tools", tools);
            
            return ResponseEntity.ok(response);
        });
    }

    /**
     * Handle MCP tools/call request.
     * Available at both /custom-mcp/tools/call and /mcp/tools/call
     */
    @PostMapping(value = {"/custom-mcp/tools/call", "/mcp/tools/call"}, 
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> callTool(
            @RequestHeader(value = "X-Toolset", required = false) String toolsetHeader,
            @RequestParam(value = "toolset", required = false) String toolsetParam,
            @RequestBody Map<String, Object> request) {
        
        return Mono.fromCallable(() -> {
            String toolset = toolsetHeader != null ? toolsetHeader 
                    : (toolsetParam != null ? toolsetParam 
                    : instanceContext.getCurrentInstance());
            
            if (toolset == null || toolset.isEmpty()) {
                toolset = "default";
            }
            
            String toolName = (String) request.get("name");
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = (Map<String, Object>) request.get("arguments");
            
            if (arguments == null) {
                arguments = new HashMap<>();
            }
            
            try {
                Object result = mcpServer.callTool(toolset, toolName, arguments);
                
                Map<String, Object> response = new HashMap<>();
                response.put("content", List.of(Map.of(
                    "type", "text",
                    "text", result != null ? result.toString() : "null"
                )));
                
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Tool execution failed: {}", e.getMessage(), e);
                
                Map<String, Object> response = new HashMap<>();
                response.put("isError", true);
                response.put("content", List.of(Map.of(
                    "type", "text",
                    "text", "Error: " + e.getMessage()
                )));
                
                return ResponseEntity.ok(response);
            }
        });
    }

    /**
     * Handle JSON-RPC requests (standard MCP protocol).
     * Available at both /custom-mcp/ and /mcp/
     */
    @PostMapping(value = {"/custom-mcp/", "/mcp/"}, 
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> handleJsonRpc(
            @RequestHeader(value = "X-Toolset", required = false) String toolsetHeader,
            @RequestParam(value = "toolset", required = false) String toolsetParam,
            @RequestBody Map<String, Object> request) {
        
        return Mono.fromCallable(() -> {
            String toolset = toolsetHeader != null ? toolsetHeader 
                    : (toolsetParam != null ? toolsetParam 
                    : instanceContext.getCurrentInstance());
            
            if (toolset == null || toolset.isEmpty()) {
                toolset = "default";
            }
            
            instanceContext.setCurrentInstance(toolset);
            mcpServer.initialize(toolset);
            
            String method = (String) request.get("method");
            Object id = request.get("id");
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) request.get("params");
            
            if (params == null) {
                params = new HashMap<>();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("jsonrpc", "2.0");
            response.put("id", id);
            
            try {
                // Check if this is a notification (no id field means it's a notification)
                boolean isNotification = id == null;
                
                Object result = null;
                
                switch (method) {
                    case "initialize":
                        result = handleInitialize(params);
                        break;
                    case "tools/list":
                        result = handleToolsList(toolset);
                        break;
                    case "tools/call":
                        result = handleToolsCall(toolset, params);
                        break;
                    case "notifications/initialized":
                        // This is a notification, no response needed
                        if (isNotification) {
                            return ResponseEntity.ok().build();
                        }
                        result = new HashMap<>(); // Empty result for requests
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown method: " + method);
                }
                
                // For notifications, don't send response
                if (isNotification && method.startsWith("notifications/")) {
                    return ResponseEntity.ok().build();
                }
                
                response.put("result", result);
            } catch (Exception e) {
                log.error("JSON-RPC error: {}", e.getMessage(), e);
                
                Map<String, Object> error = new HashMap<>();
                error.put("code", -32603);
                error.put("message", "Internal error: " + e.getMessage());
                response.put("error", error);
            }
            
            return ResponseEntity.ok(response);
        });
    }

    private Map<String, Object> handleInitialize(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2025-06-18");
        
        Map<String, Object> capabilities = new HashMap<>();
        Map<String, Object> tools = new HashMap<>();
        tools.put("listChanged", false);
        capabilities.put("tools", tools);
        result.put("capabilities", capabilities);
        
        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("name", "custom-mcp-server");
        serverInfo.put("version", "1.0.0");
        result.put("serverInfo", serverInfo);
        
        return result;
    }

    private Map<String, Object> handleToolsList(String toolset) {
        List<Map<String, Object>> tools = mcpServer.listTools(toolset);
        Map<String, Object> result = new HashMap<>();
        result.put("tools", tools);
        return result;
    }

    private Map<String, Object> handleToolsCall(String toolset, Map<String, Object> params) {
        String toolName = (String) params.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
        
        if (arguments == null) {
            arguments = new HashMap<>();
        }
        
        Object result = mcpServer.callTool(toolset, toolName, arguments);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", List.of(Map.of(
            "type", "text",
            "text", result != null ? result.toString() : "null"
        )));
        
        return response;
    }

    /**
     * Test endpoint to verify custom MCP Server is working.
     */
    @GetMapping(value = {"/custom-mcp/test", "/mcp/test"}, 
                produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> test(
            @RequestHeader(value = "X-Toolset", required = false) String toolsetHeader,
            @RequestParam(value = "toolset", required = false) String toolsetParam) {
        
        return Mono.fromCallable(() -> {
            String toolset = toolsetHeader != null ? toolsetHeader 
                    : (toolsetParam != null ? toolsetParam : "example-tools");
            
            if (toolset == null || toolset.isEmpty()) {
                toolset = "example-tools";
            }
            
            instanceContext.setCurrentInstance(toolset);
            mcpServer.initialize(toolset);
            
            List<Map<String, Object>> tools = mcpServer.listTools(toolset);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ok");
            response.put("toolset", toolset);
            response.put("toolCount", tools.size());
            response.put("tools", tools);
            response.put("message", "Custom MCP Server is working!");
            
            return ResponseEntity.ok(response);
        });
    }
}

