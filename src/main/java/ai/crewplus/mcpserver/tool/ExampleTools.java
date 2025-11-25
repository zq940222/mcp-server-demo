package ai.crewplus.mcpserver.tool;

import ai.crewplus.mcpserver.service.InstanceContext;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;

/**
 * Example tools for demonstration.
 * These tools check the instance parameter before execution.
 * 
 * Note: This class is NOT annotated with @Service to prevent Spring AI MCP Server
 * from auto-scanning and registering all @McpTool methods at startup.
 * Tools will be registered dynamically via McpServerConfig based on instance parameter.
 */
public class ExampleTools {

    private InstanceContext instanceContext;

    public void setInstanceContext(InstanceContext instanceContext) {
        this.instanceContext = instanceContext;
    }

    /**
     * Check if this tool should be available for the current instance.
     */
    private boolean isToolAvailable(String toolName) {
        if (instanceContext == null) {
            return true; // If no context, allow all tools
        }
        
        String instance = instanceContext.getCurrentInstance();
        if (instance == null) {
            instance = "default";
        }
        
        String instanceKey = instance.toLowerCase();
        
        switch (instanceKey) {
            case "example1":
            case "instance1":
                return toolName.equals("calculator") || 
                       toolName.equals("greeting") || 
                       toolName.equals("getCurrentTime");
            case "example2":
            case "instance2":
                return false; // Example2Tools only
            case "all":
            case "both":
                return true;
            default:
                return toolName.equals("calculator") || 
                       toolName.equals("greeting") || 
                       toolName.equals("getCurrentTime");
        }
    }

    /**
     * Calculator tool that performs basic arithmetic operations.
     *
     * @param operation The operation to perform (add, subtract, multiply, divide)
     * @param num1 First number
     * @param num2 Second number
     * @return Calculation result
     */
    @McpTool(description = "Perform basic arithmetic operations (add, subtract, multiply, divide)")
    public String calculator(
            @McpToolParam(description = "The operation to perform (add, subtract, multiply, divide)", required = true) String operation,
            @McpToolParam(description = "First number", required = true) double num1,
            @McpToolParam(description = "Second number", required = true) double num2) {
        if (!isToolAvailable("calculator")) {
            return "Error: This tool is not available for the current instance.";
        }
        
        try {
            double result;
            switch (operation.toLowerCase()) {
                case "add":
                    result = num1 + num2;
                    break;
                case "subtract":
                    result = num1 - num2;
                    break;
                case "multiply":
                    result = num1 * num2;
                    break;
                case "divide":
                    if (num2 == 0) {
                        return "Error: Division by zero";
                    }
                    result = num1 / num2;
                    break;
                default:
                    return "Unknown operation. Supported: add, subtract, multiply, divide";
            }
            return String.format("%.2f %s %.2f = %.2f", num1, operation, num2, result);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Greeting tool that greets users.
     *
     * @param name User name
     * @return Greeting message
     */
    @McpTool(description = "Greet users with a personalized message")
    public String greeting(
            @McpToolParam(description = "User name", required = false) String name) {
        if (!isToolAvailable("greeting")) {
            return "Error: This tool is not available for the current instance.";
        }
        
        if (name == null || name.trim().isEmpty()) {
            return "Hello, anonymous user!";
        }
        return "Hello, " + name.trim() + "! Welcome to the MCP Server.";
    }

    /**
     * Time tool that returns current timestamp.
     *
     * @return Current date and time
     */
    @McpTool(description = "Get current date and time")
    public String getCurrentTime() {
        if (!isToolAvailable("getCurrentTime")) {
            return "Error: This tool is not available for the current instance.";
        }
        
        return "Current time: " + java.time.LocalDateTime.now().toString();
    }
}
