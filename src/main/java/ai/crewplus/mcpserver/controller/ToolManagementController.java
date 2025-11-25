package ai.crewplus.mcpserver.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for managing tools dynamically.
 * Provides endpoints to query tool registration status.
 * 
 * Note: Actual tool registration is handled by McpServerLifecycleHandler when MCP sessions are created.
 * Tools are registered dynamically based on the instance parameter in the session initialization.
 */
@RestController
@RequestMapping("/api/tools")
public class ToolManagementController {

    /**
     * Get tool information for a specific instance.
     * Returns which tools would be registered for the given instance.
     *
     * @param instance Instance identifier
     * @return Map containing instance and expected tools
     */
    @GetMapping("/instance/{instance}")
    public ResponseEntity<Map<String, Object>> getToolsForInstance(@PathVariable String instance) {
        Map<String, Object> response = new HashMap<>();
        response.put("instance", instance);
        
        String instanceKey = (instance == null || instance.trim().isEmpty()) 
                ? "default" 
                : instance.trim().toLowerCase();
        
        // Return expected tools for this instance
        switch (instanceKey) {
            case "example1":
            case "instance1":
                response.put("tools", java.util.List.of("calculator", "greeting", "getCurrentTime"));
                break;
            case "example2":
            case "instance2":
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
     * Health check endpoint.
     *
     * @return Status message
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "Tool registration service is running. Tools are registered dynamically based on instance parameter.");
        return ResponseEntity.ok(response);
    }
}

