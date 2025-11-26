package ai.crewplus.mcpserver.service;

import ai.crewplus.mcpserver.tool.ExampleTools;
import ai.crewplus.mcpserver.tool.Example2Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic toolset loader for loading toolsets at runtime.
 * Supports loading toolsets from classpath or external JAR files.
 * 
 * This component implements the SPI mechanism for dynamic toolset loading,
 * allowing new toolsets to be loaded without restarting the service.
 */
@Component
public class DynamicToolsetLoader {

    private static final Logger log = LoggerFactory.getLogger(DynamicToolsetLoader.class);

    private final InstanceContext instanceContext;
    private final List<String> allowedToolsets;

    public DynamicToolsetLoader(InstanceContext instanceContext,
                               @Value("${mcp.toolset.allowed:example-tools,example2-tools,order-tools,weather-tools,payment-tools}") 
                               List<String> allowedToolsets) {
        this.instanceContext = instanceContext;
        this.allowedToolsets = allowedToolsets != null ? allowedToolsets : new ArrayList<>();
    }

    /**
     * Load a toolset dynamically by name.
     * First tries to load from predefined toolsets, then attempts dynamic class loading.
     *
     * @param toolsetName Toolset identifier
     * @return List of tool objects for this toolset
     * @throws SecurityException if toolset is not in whitelist
     * @throws ClassNotFoundException if toolset class cannot be found
     */
    public List<Object> loadToolset(String toolsetName) throws ClassNotFoundException, SecurityException {
        String normalizedName = toolsetName.trim().toLowerCase();
        
        // Security check: verify toolset is in whitelist
        if (!isToolsetAllowed(normalizedName)) {
            log.warn("Toolset {} is not in allowed list, rejecting", normalizedName);
            throw new SecurityException("Toolset " + normalizedName + " is not allowed");
        }

        // Try predefined toolsets first
        List<Object> predefinedTools = loadPredefinedToolset(normalizedName);
        if (predefinedTools != null && !predefinedTools.isEmpty()) {
            return predefinedTools;
        }

        // Try dynamic class loading
        return loadToolsetFromClass(normalizedName);
    }

    /**
     * Load predefined toolsets (ExampleTools, Example2Tools, etc.).
     *
     * @param toolsetName Toolset identifier
     * @return List of tool objects or null if not found
     */
    private List<Object> loadPredefinedToolset(String toolsetName) {
        List<Object> tools = new ArrayList<>();
        
        switch (toolsetName) {
            case "example-tools":
            case "example1":
            case "instance1":
                ExampleTools exampleTools = new ExampleTools();
                exampleTools.setInstanceContext(instanceContext);
                tools.add(exampleTools);
                log.debug("Loaded predefined toolset: example-tools");
                return tools;

            case "example2-tools":
            case "example2":
            case "instance2":
                Example2Tools example2Tools = new Example2Tools();
                example2Tools.setInstanceContext(instanceContext);
                tools.add(example2Tools);
                log.debug("Loaded predefined toolset: example2-tools");
                return tools;

            case "all":
            case "both":
                ExampleTools et = new ExampleTools();
                et.setInstanceContext(instanceContext);
                Example2Tools e2t = new Example2Tools();
                e2t.setInstanceContext(instanceContext);
                tools.add(et);
                tools.add(e2t);
                log.debug("Loaded predefined toolset: all");
                return tools;

            default:
                return null;
        }
    }

    /**
     * Load toolset from class name dynamically.
     * Uses reflection to instantiate toolset classes.
     *
     * @param toolsetName Toolset identifier (can be class name)
     * @return List of tool objects
     * @throws ClassNotFoundException if class cannot be found
     */
    private List<Object> loadToolsetFromClass(String toolsetName) throws ClassNotFoundException {
        // Try to construct class name from toolset name
        // e.g., "order-tools" -> "com.example.OrderTools" or "ai.crewplus.mcpserver.tool.OrderTools"
        String className = constructClassName(toolsetName);
        
        try {
            Class<?> toolsetClass = Class.forName(className);
            Object instance = toolsetClass.getDeclaredConstructor().newInstance();
            
            // Try to set InstanceContext if method exists
            try {
                java.lang.reflect.Method setContextMethod = toolsetClass.getMethod("setInstanceContext", InstanceContext.class);
                setContextMethod.invoke(instance, instanceContext);
            } catch (NoSuchMethodException e) {
                // Method doesn't exist, that's okay
                log.debug("Toolset class {} does not have setInstanceContext method", className);
            }
            
            List<Object> tools = new ArrayList<>();
            tools.add(instance);
            log.info("Dynamically loaded toolset class: {}", className);
            return tools;
            
        } catch (ClassNotFoundException e) {
            log.debug("Toolset class {} not found, trying alternative names", className);
            // Try alternative class names
            return tryAlternativeClassNames(toolsetName);
        } catch (Exception e) {
            log.error("Failed to instantiate toolset class {}: {}", className, e.getMessage());
            throw new RuntimeException("Failed to load toolset: " + toolsetName, e);
        }
    }

    /**
     * Construct class name from toolset name.
     *
     * @param toolsetName Toolset identifier
     * @return Fully qualified class name
     */
    private String constructClassName(String toolsetName) {
        // Convert "order-tools" to "OrderTools"
        String[] parts = toolsetName.split("-");
        StringBuilder className = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                className.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    className.append(part.substring(1));
                }
            }
        }
        
        // Try common package patterns
        String[] packagePrefixes = {
            "ai.crewplus.mcpserver.tool.",
            "com.example.",
            ""
        };
        
        for (String prefix : packagePrefixes) {
            String fullClassName = prefix + className.toString();
            try {
                Class.forName(fullClassName);
                return fullClassName;
            } catch (ClassNotFoundException e) {
                // Try next prefix
            }
        }
        
        // Return default pattern
        return "ai.crewplus.mcpserver.tool." + className.toString();
    }

    /**
     * Try alternative class names for toolset loading.
     *
     * @param toolsetName Toolset identifier
     * @return List of tool objects
     * @throws ClassNotFoundException if no alternative class found
     */
    private List<Object> tryAlternativeClassNames(String toolsetName) throws ClassNotFoundException {
        // Try direct class name match
        String[] alternatives = {
            toolsetName,
            "ai.crewplus.mcpserver.tool." + toolsetName,
            "com.example." + toolsetName
        };
        
        for (String className : alternatives) {
            try {
                Class<?> toolsetClass = Class.forName(className);
                Object instance = toolsetClass.getDeclaredConstructor().newInstance();
                
                // Try to set InstanceContext
                try {
                    java.lang.reflect.Method setContextMethod = toolsetClass.getMethod("setInstanceContext", InstanceContext.class);
                    setContextMethod.invoke(instance, instanceContext);
                } catch (NoSuchMethodException e) {
                    // Ignore
                }
                
                List<Object> tools = new ArrayList<>();
                tools.add(instance);
                return tools;
            } catch (ClassNotFoundException e) {
                // Try next alternative
            } catch (Exception e) {
                log.warn("Failed to load alternative class {}: {}", className, e.getMessage());
            }
        }
        
        throw new ClassNotFoundException("Toolset class not found for: " + toolsetName);
    }

    /**
     * Load toolset from external JAR file.
     *
     * @param jarPath Path to JAR file
     * @param className Fully qualified class name
     * @return Toolset instance
     * @throws Exception if loading fails
     */
    public Object loadToolsetFromJar(String jarPath, String className) throws Exception {
        File jarFile = new File(jarPath);
        if (!jarFile.exists()) {
            throw new IllegalArgumentException("JAR file not found: " + jarPath);
        }
        
        URL jarUrl = jarFile.toURI().toURL();
        URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl}, 
                                                         Thread.currentThread().getContextClassLoader());
        
        try {
            Class<?> toolsetClass = classLoader.loadClass(className);
            Object instance = toolsetClass.getDeclaredConstructor().newInstance();
            
            // Try to set InstanceContext
            try {
                java.lang.reflect.Method setContextMethod = toolsetClass.getMethod("setInstanceContext", InstanceContext.class);
                setContextMethod.invoke(instance, instanceContext);
            } catch (NoSuchMethodException e) {
                // Ignore
            }
            
            log.info("Loaded toolset from JAR: {} -> {}", jarPath, className);
            return instance;
        } finally {
            classLoader.close();
        }
    }

    /**
     * Check if toolset is allowed (whitelist check).
     *
     * @param toolsetName Toolset identifier
     * @return true if toolset is allowed
     */
    public boolean isToolsetAllowed(String toolsetName) {
        if (allowedToolsets.isEmpty()) {
            // If whitelist is empty, allow all (for development)
            log.warn("No toolset whitelist configured, allowing all toolsets");
            return true;
        }
        
        String normalizedName = toolsetName.trim().toLowerCase();
        return allowedToolsets.stream()
                .anyMatch(allowed -> allowed.trim().toLowerCase().equals(normalizedName));
    }

    /**
     * Get allowed toolsets list.
     *
     * @return List of allowed toolset names
     */
    public List<String> getAllowedToolsets() {
        return new ArrayList<>(allowedToolsets);
    }
}

