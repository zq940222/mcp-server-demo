package ai.crewplus.mcpserver.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic tool registry service for managing tools at runtime.
 * This service allows adding and removing tool objects dynamically.
 */
@Service
public class DynamicToolRegistry {

    private final Map<String, Object> registeredToolObjects = new ConcurrentHashMap<>();

    /**
     * Register a tool object dynamically.
     *
     * @param name Tool name
     * @param toolObject Tool object containing @Tool annotated methods
     */
    public void registerTool(String name, Object toolObject) {
        registeredToolObjects.put(name, toolObject);
    }

    /**
     * Unregister a tool.
     *
     * @param name Tool name to unregister
     * @return true if tool was removed, false if not found
     */
    public boolean unregisterTool(String name) {
        return registeredToolObjects.remove(name) != null;
    }

    /**
     * Get all registered tool objects.
     *
     * @return Map of registered tool objects
     */
    public Map<String, Object> getRegisteredTools() {
        return Map.copyOf(registeredToolObjects);
    }

    /**
     * Get all registered tool objects as array.
     *
     * @return Array of registered tool objects
     */
    public Object[] getRegisteredToolObjects() {
        return registeredToolObjects.values().toArray();
    }

    /**
     * Check if a tool is registered.
     *
     * @param name Tool name
     * @return true if tool is registered
     */
    public boolean isToolRegistered(String name) {
        return registeredToolObjects.containsKey(name);
    }

    /**
     * Get all registered tool names.
     *
     * @return Set of registered tool names
     */
    public Set<String> getRegisteredToolNames() {
        return Set.copyOf(registeredToolObjects.keySet());
    }

    /**
     * Clear all registered tools.
     */
    public void clearAllTools() {
        registeredToolObjects.clear();
    }
}

