package ai.crewplus.mcpserver.controller;

import ai.crewplus.mcpserver.service.ToolsetRouter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for managing tools dynamically.
 * Provides endpoints to query tool registration status and toolset information.
 * 
 * Note: Actual tool registration is handled dynamically based on the toolset parameter
 * from HTTP Header (X-Toolset), query parameter (?toolset=xxx), or request body.
 */
@RestController
@RequestMapping("/api/tools")
public class ToolManagementController {

    private final ToolsetRouter toolsetRouter;

    public ToolManagementController(ToolsetRouter toolsetRouter) {
        this.toolsetRouter = toolsetRouter;
    }

    /**
     * Get tool information for a specific instance/toolset.
     * Returns which tools would be registered for the given instance/toolset.
     *
     * @param instance Instance or toolset identifier
     * @return Map containing instance/toolset and expected tools
     */
    @GetMapping("/instance/{instance}")
    public ResponseEntity<Map<String, Object>> getToolsForInstance(@PathVariable String instance) {
        Map<String, Object> response = new HashMap<>();
        response.put("instance", instance);
        
        String instanceKey = (instance == null || instance.trim().isEmpty()) 
                ? "default" 
                : instance.trim().toLowerCase();
        
        // Return expected tools for this instance/toolset
        switch (instanceKey) {
            case "example1":
            case "instance1":
            case "example-tools":
                response.put("tools", java.util.List.of("calculator", "greeting", "getCurrentTime"));
                break;
            case "example2":
            case "instance2":
            case "example2-tools":
                response.put("tools", java.util.List.of("getWeather", "convertTemperature", "generateRandomNumber", "reverseString"));
                break;
            case "all":
            case "both":
                response.put("tools", java.util.List.of("calculator", "greeting", "getCurrentTime", 
                        "getWeather", "convertTemperature", "generateRandomNumber", "reverseString"));
                break;
            default:
                response.put("tools", java.util.List.of("calculator", "greeting", "getCurrentTime"));
                break;
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get toolset information.
     * Returns registered toolsets and their status.
     *
     * @return Map containing toolset information
     */
    @GetMapping("/toolsets")
    public ResponseEntity<Map<String, Object>> getToolsets() {
        Map<String, Object> response = new HashMap<>();
        response.put("registeredToolsets", toolsetRouter.getRegisteredToolsets());
        response.put("cacheSize", toolsetRouter.getCacheSize());
        return ResponseEntity.ok(response);
    }

    /**
     * Get tool information for a specific toolset.
     *
     * @param toolset Toolset identifier
     * @return Map containing toolset and expected tools
     */
    @GetMapping("/toolset/{toolset}")
    public ResponseEntity<Map<String, Object>> getToolsForToolset(@PathVariable String toolset) {
        Map<String, Object> response = new HashMap<>();
        response.put("toolset", toolset);
        response.put("isRegistered", toolsetRouter.isToolsetRegistered(toolset));
        
        // Get tool objects for this toolset
        try {
            var toolObjects = toolsetRouter.getToolObjectsForToolset(toolset);
            response.put("toolCount", toolObjects.size());
            response.put("status", "loaded");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint.
     *
     * @return Status message
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "Tool registration service is running. Tools are registered dynamically based on toolset parameter (Header: X-Toolset, Query: ?toolset=xxx, or Body: {\"toolset\": \"xxx\"}).");
        return ResponseEntity.ok(response);
    }
}

