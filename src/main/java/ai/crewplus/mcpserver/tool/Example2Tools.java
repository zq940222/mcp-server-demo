package ai.crewplus.mcpserver.tool;

import ai.crewplus.mcpserver.service.InstanceContext;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;

/**
 * Example 2 tools for demonstration.
 * These tools check the instance parameter before execution.
 * 
 * Note: This class is NOT annotated with @Service to prevent Spring AI MCP Server
 * from auto-scanning and registering all @McpTool methods at startup.
 * Tools will be registered dynamically via McpServerConfig based on instance parameter.
 */
public class Example2Tools {

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
                return false; // ExampleTools only
            case "example2":
            case "instance2":
                return toolName.equals("getWeather") || 
                       toolName.equals("convertTemperature") || 
                       toolName.equals("generateRandomNumber") || 
                       toolName.equals("reverseString");
            case "all":
            case "both":
                return true;
            default:
                return false; // Default: ExampleTools only
        }
    }

    /**
     * Weather tool that returns weather information for a location.
     *
     * @param location Location name
     * @return Weather information
     */
    @McpTool(description = "Get weather information for a specific location")
    public String getWeather(
            @McpToolParam(description = "Location name (e.g., city name)", required = true) String location) {
        if (!isToolAvailable("getWeather")) {
            return "Error: This tool is not available for the current instance.";
        }
        
        return String.format("Weather in %s: Sunny, 25°C, Humidity: 60%%", location);
    }

    /**
     * Convert temperature between Celsius and Fahrenheit.
     *
     * @param temperature Temperature value
     * @param fromUnit Source unit (Celsius or Fahrenheit)
     * @param toUnit Target unit (Celsius or Fahrenheit)
     * @return Converted temperature
     */
    @McpTool(description = "Convert temperature between Celsius and Fahrenheit")
    public String convertTemperature(
            @McpToolParam(description = "Temperature value", required = true) double temperature,
            @McpToolParam(description = "Source unit (Celsius or Fahrenheit)", required = true) String fromUnit,
            @McpToolParam(description = "Target unit (Celsius or Fahrenheit)", required = true) String toUnit) {
        if (!isToolAvailable("convertTemperature")) {
            return "Error: This tool is not available for the current instance.";
        }
        
        try {
            double result;
            if (fromUnit.equalsIgnoreCase("Celsius") && toUnit.equalsIgnoreCase("Fahrenheit")) {
                result = (temperature * 9.0 / 5.0) + 32;
            } else if (fromUnit.equalsIgnoreCase("Fahrenheit") && toUnit.equalsIgnoreCase("Celsius")) {
                result = (temperature - 32) * 5.0 / 9.0;
            } else if (fromUnit.equalsIgnoreCase(toUnit)) {
                result = temperature;
            } else {
                return "Error: Unsupported unit conversion. Supported units: Celsius, Fahrenheit";
            }
            return String.format("%.2f° %s = %.2f° %s", temperature, fromUnit, result, toUnit);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Generate a random number within a range.
     *
     * @param min Minimum value
     * @param max Maximum value
     * @return Random number
     */
    @McpTool(description = "Generate a random number within a specified range")
    public String generateRandomNumber(
            @McpToolParam(description = "Minimum value", required = true) int min,
            @McpToolParam(description = "Maximum value", required = true) int max) {
        if (!isToolAvailable("generateRandomNumber")) {
            return "Error: This tool is not available for the current instance.";
        }
        
        if (min >= max) {
            return "Error: Minimum value must be less than maximum value";
        }
        int random = (int) (Math.random() * (max - min + 1)) + min;
        return String.format("Random number between %d and %d: %d", min, max, random);
    }

    /**
     * Reverse a string.
     *
     * @param text Text to reverse
     * @return Reversed text
     */
    @McpTool(description = "Reverse a given string")
    public String reverseString(
            @McpToolParam(description = "Text to reverse", required = true) String text) {
        if (!isToolAvailable("reverseString")) {
            return "Error: This tool is not available for the current instance.";
        }
        
        if (text == null) {
            return "Error: Text cannot be null";
        }
        return new StringBuilder(text).reverse().toString();
    }
}
