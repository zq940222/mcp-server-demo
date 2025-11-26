package ai.crewplus.mcpserver.tool;

import ai.crewplus.mcpserver.annotation.DynamicToolset;
import ai.crewplus.mcpserver.service.InstanceContext;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;

/**
 * Example 2 tools for demonstration.
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
@DynamicToolset(value = {"example2-tools", "example2", "instance2"},
                name = "Example2 Tools",
                description = "Advanced example tools: weather, temperature conversion, random number, string reverse")
public class Example2Tools {

    private InstanceContext instanceContext;

    public void setInstanceContext(InstanceContext instanceContext) {
        this.instanceContext = instanceContext;
    }

    /**
     * Check if this tool should be available for the current instance/toolset.
     */
    private boolean isToolAvailable(String toolName) {
        if (instanceContext == null) {
            return true; // If no context, allow all tools
        }
        
        String instanceOrToolset = instanceContext.getCurrentInstance();
        if (instanceOrToolset == null) {
            instanceOrToolset = "default";
        }
        
        String key = instanceOrToolset.toLowerCase();
        
        switch (key) {
            case "example1":
            case "instance1":
            case "example-tools":
                return false; // ExampleTools only
            case "example2":
            case "instance2":
            case "example2-tools":
                return toolName.equals("getWeather") || 
                       toolName.equals("convertTemperature") || 
                       toolName.equals("generateRandomNumber") || 
                       toolName.equals("reverseString");
            case "all":
            case "both":
                return true;
            default:
                // For unknown toolsets, return false to prevent tool availability
                // Only allow if explicitly matches example2-tools
                return key.equals("example2-tools") && 
                       (toolName.equals("getWeather") || 
                        toolName.equals("convertTemperature") || 
                        toolName.equals("generateRandomNumber") || 
                        toolName.equals("reverseString"));
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
