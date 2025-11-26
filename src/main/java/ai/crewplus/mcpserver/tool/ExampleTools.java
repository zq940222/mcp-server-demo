package ai.crewplus.mcpserver.tool;

import ai.crewplus.mcpserver.annotation.DynamicToolset;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;

/**
 * Example tools for demonstration.
 * 
 * This tool class is annotated with @DynamicToolset to enable dynamic discovery.
 * Tools will NOT be registered at startup, but will be discovered and registered
 * dynamically at connection time when the toolset parameter matches.
 * 
 * @DynamicToolset annotation enables true dynamic discovery:
 * - Tools are NOT scanned at startup
 * - Tools are discovered and registered at connection time
 * - Only tools matching the toolset parameter are registered
 */
@DynamicToolset(value = {"example-tools", "example1", "instance1"}, 
                name = "Example Tools",
                description = "Basic example tools: calculator, greeting, getCurrentTime")
public class ExampleTools {

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
        return "Current time: " + java.time.LocalDateTime.now().toString();
    }
}
