package ai.crewplus.mcpserver.mcp;

import ai.crewplus.mcpserver.hub.McpHub;
import ai.crewplus.mcpserver.hub.McpInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom MCP Server implementation that supports true dynamic tool discovery.
 * 
 * This server bypasses Spring AI MCP Server's limitations by implementing
 * the MCP protocol directly, allowing runtime dynamic tool registration.
 */
@Component
public class CustomMcpServer {

    private static final Logger log = LoggerFactory.getLogger(CustomMcpServer.class);

    private final McpHub mcpHub;
    
    // Cache of tool methods by toolset
    private final Map<String, List<ToolDefinition>> toolCache = new ConcurrentHashMap<>();

    @Autowired
    public CustomMcpServer(McpHub mcpHub) {
        this.mcpHub = mcpHub;
    }

    /**
     * Initialize the server with the given toolset.
     * This loads tools for the toolset and caches them.
     */
    public void initialize(String toolset) {
        // Skip if already initialized
        if (toolCache.containsKey(toolset)) {
            List<ToolDefinition> cached = toolCache.get(toolset);
            if (cached != null && !cached.isEmpty()) {
                return;
            }
        }
        
        // Get instance from hub
        McpInstance instance = mcpHub.getInstance(toolset);
        if (instance == null) {
            log.warn("McpHub.getInstance() returned null for toolset: {}", toolset);
            toolCache.put(toolset, Collections.emptyList());
            return;
        }
        
        if (!instance.hasTools()) {
            log.warn("Instance found but has no tools for toolset: {}", toolset);
            toolCache.put(toolset, Collections.emptyList());
            return;
        }

        // Extract tool definitions from tool objects
        List<ToolDefinition> tools = extractToolDefinitions(instance.getTools());
        toolCache.put(toolset, tools);
    }

    /**
     * Get list of available tools for the current toolset.
     */
    public List<Map<String, Object>> listTools(String toolset) {
        List<ToolDefinition> tools = toolCache.getOrDefault(toolset, Collections.emptyList());
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (ToolDefinition tool : tools) {
            Map<String, Object> toolMap = new HashMap<>();
            toolMap.put("name", tool.getName());
            toolMap.put("description", tool.getDescription());
            toolMap.put("inputSchema", tool.getInputSchema());
            result.add(toolMap);
        }
        
        return result;
    }

    /**
     * Call a tool by name.
     */
    public Object callTool(String toolset, String toolName, Map<String, Object> arguments) {
        // Ensure toolset is initialized
        if (!toolCache.containsKey(toolset)) {
            initialize(toolset);
        }
        
        List<ToolDefinition> tools = toolCache.getOrDefault(toolset, Collections.emptyList());
        
        if (tools.isEmpty()) {
            log.warn("No tools found for toolset: {}", toolset);
            throw new IllegalArgumentException("No tools available for toolset: " + toolset);
        }
        
        for (ToolDefinition tool : tools) {
            if (tool.getName().equals(toolName)) {
                try {
                    return tool.invoke(arguments);
                } catch (Exception e) {
                    log.error("Failed to invoke tool {}: {}", toolName, e.getMessage(), e);
                    throw new RuntimeException("Tool execution failed: " + e.getMessage(), e);
                }
            }
        }
        
        log.warn("Tool {} not found in toolset: {}", toolName, toolset);
        throw new IllegalArgumentException("Tool not found: " + toolName);
    }

    /**
     * Extract tool definitions from tool objects.
     */
    private List<ToolDefinition> extractToolDefinitions(List<Object> toolObjects) {
        List<ToolDefinition> definitions = new ArrayList<>();
        
        for (Object toolObject : toolObjects) {
            Class<?> toolClass = toolObject.getClass();
            Method[] methods = toolClass.getDeclaredMethods();
            
            for (Method method : methods) {
                if (method.isAnnotationPresent(
                        org.springaicommunity.mcp.annotation.McpTool.class)) {
                    try {
                        ToolDefinition definition = createToolDefinition(
                                toolObject, method);
                        definitions.add(definition);
                    } catch (Exception e) {
                        log.warn("⚠️ Failed to create tool definition for method {}: {}", 
                                method.getName(), e.getMessage());
                    }
                }
            }
        }
        
        return definitions;
    }

    /**
     * Create a ToolDefinition from a method.
     */
    private ToolDefinition createToolDefinition(Object toolObject, Method method) {
        org.springaicommunity.mcp.annotation.McpTool annotation = 
                method.getAnnotation(org.springaicommunity.mcp.annotation.McpTool.class);
        
        String name = annotation.name().isEmpty() ? method.getName() : annotation.name();
        String description = annotation.description().isEmpty() 
                ? "Tool: " + method.getName() 
                : annotation.description();
        
        // Build input schema from method parameters
        Map<String, Object> inputSchema = buildInputSchema(method, annotation);
        
        return new ToolDefinition(name, description, inputSchema, toolObject, method);
    }

    /**
     * Build JSON Schema for method parameters.
     */
    private Map<String, Object> buildInputSchema(Method method, 
            org.springaicommunity.mcp.annotation.McpTool annotation) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();
        
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        for (java.lang.reflect.Parameter param : parameters) {
            // Skip InstanceContext parameter
            if (param.getType().getName().contains("InstanceContext")) {
                continue;
            }
            
            String paramName = param.getName();
            Map<String, Object> paramSchema = new HashMap<>();
            
            // Try to get @McpToolParam annotation
            org.springaicommunity.mcp.annotation.McpToolParam paramAnnotation = 
                    param.getAnnotation(org.springaicommunity.mcp.annotation.McpToolParam.class);
            
            if (paramAnnotation != null) {
                // McpToolParam doesn't have name() method, use parameter name
                if (!paramAnnotation.description().isEmpty()) {
                    paramSchema.put("description", paramAnnotation.description());
                }
            }
            
            // Determine parameter type
            Class<?> paramType = param.getType();
            if (paramType == String.class) {
                paramSchema.put("type", "string");
            } else if (paramType == Integer.class || paramType == int.class) {
                paramSchema.put("type", "integer");
            } else if (paramType == Double.class || paramType == double.class ||
                       paramType == Float.class || paramType == float.class) {
                paramSchema.put("type", "number");
            } else if (paramType == Boolean.class || paramType == boolean.class) {
                paramSchema.put("type", "boolean");
            } else {
                paramSchema.put("type", "string"); // Default to string
            }
            
            properties.put(paramName, paramSchema);
            
            if (paramAnnotation == null || paramAnnotation.required()) {
                required.add(paramName);
            }
        }
        
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        
        return schema;
    }

    /**
     * Tool definition.
     */
    public static class ToolDefinition {
        private final String name;
        private final String description;
        private final Map<String, Object> inputSchema;
        private final Object toolObject;
        private final Method method;

        public ToolDefinition(String name, String description, 
                             Map<String, Object> inputSchema,
                             Object toolObject, Method method) {
            this.name = name;
            this.description = description;
            this.inputSchema = inputSchema;
            this.toolObject = toolObject;
            this.method = method;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public Map<String, Object> getInputSchema() { return inputSchema; }

        public Object invoke(Map<String, Object> arguments) throws Exception {
            // Prepare method arguments
            java.lang.reflect.Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];
            
            for (int i = 0; i < parameters.length; i++) {
                java.lang.reflect.Parameter param = parameters[i];
                
                // Skip InstanceContext parameter
                if (param.getType().getName().contains("InstanceContext")) {
                    args[i] = null; // Will be injected if needed
                    continue;
                }
                
                String paramName = param.getName();
                org.springaicommunity.mcp.annotation.McpToolParam paramAnnotation = 
                        param.getAnnotation(org.springaicommunity.mcp.annotation.McpToolParam.class);
                // McpToolParam doesn't have name() method, use parameter name
                
                Object value = arguments.get(paramName);
                if (value == null && paramAnnotation != null && paramAnnotation.required()) {
                    throw new IllegalArgumentException("Required parameter missing: " + paramName);
                }
                
                // Convert value to parameter type
                args[i] = convertValue(value, param.getType());
            }
            
            method.setAccessible(true);
            return method.invoke(toolObject, args);
        }

        private Object convertValue(Object value, Class<?> targetType) {
            if (value == null) {
                return null;
            }
            
            if (targetType.isAssignableFrom(value.getClass())) {
                return value;
            }
            
            // Type conversion
            if (targetType == String.class) {
                return value.toString();
            } else if (targetType == Integer.class || targetType == int.class) {
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
                return Integer.parseInt(value.toString());
            } else if (targetType == Double.class || targetType == double.class) {
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                return Double.parseDouble(value.toString());
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                if (value instanceof Boolean) {
                    return value;
                }
                return Boolean.parseBoolean(value.toString());
            }
            
            return value;
        }
    }
}

