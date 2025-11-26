package ai.crewplus.mcpserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Toolset router for dynamic toolset routing at runtime.
 * Routes requests to different toolsets based on the toolset parameter from the request.
 * 
 * This component acts as a smart dispatcher that routes each request to the correct toolset
 * processing pipeline based on runtime parameters (e.g., HTTP Header, request body, query params).
 */
@Component
public class ToolsetRouter {

    private static final Logger log = LoggerFactory.getLogger(ToolsetRouter.class);

    // Cache for toolset providers (thread-safe)
    private final Map<String, List<Object>> toolsetCache = new ConcurrentHashMap<>();
    
    private final InstanceToolManager instanceToolManager;
    private final DynamicToolsetLoader toolsetLoader;
    private final ToolsetPool toolsetPool;

    public ToolsetRouter(InstanceToolManager instanceToolManager, 
                        DynamicToolsetLoader toolsetLoader,
                        ToolsetPool toolsetPool) {
        this.instanceToolManager = instanceToolManager;
        this.toolsetLoader = toolsetLoader;
        this.toolsetPool = toolsetPool;
    }

    /**
     * Register a toolset dynamically.
     *
     * @param toolsetName Toolset identifier
     * @param toolObjects List of tool objects for this toolset
     */
    public void registerToolset(String toolsetName, List<Object> toolObjects) {
        if (toolsetName == null || toolsetName.trim().isEmpty()) {
            log.warn("Cannot register toolset with empty name");
            return;
        }
        
        String normalizedName = normalizeToolsetName(toolsetName);
        toolsetCache.put(normalizedName, toolObjects);
        log.info("Registered toolset: {} with {} tools", normalizedName, toolObjects.size());
    }

    /**
     * Get tool objects for a specific toolset at runtime.
     * This method performs dynamic routing based on the toolset parameter.
     * Uses ToolsetPool for caching optimization.
     *
     * @param toolset Toolset identifier from request (Header, body, or query param)
     * @return List of tool objects for this toolset
     */
    public List<Object> getToolObjectsForToolset(String toolset) {
        if (toolset == null || toolset.trim().isEmpty()) {
            log.debug("No toolset specified, using default");
            return getDefaultToolset();
        }

        String normalizedToolset = normalizeToolsetName(toolset);
        
        // Use ToolsetPool for caching and loading
        return toolsetPool.getOrLoad(normalizedToolset, toolsetName -> {
            // Try to load dynamically
            try {
                List<Object> loadedTools = toolsetLoader.loadToolset(toolsetName);
                if (loadedTools != null && !loadedTools.isEmpty()) {
                    log.info("Dynamically loaded toolset: {}", toolsetName);
                    return loadedTools;
                }
            } catch (Exception e) {
                log.warn("Failed to load toolset {}: {}", toolsetName, e.getMessage());
            }

            // Fallback: try to map toolset to instance-based toolset
            List<Object> instanceTools = instanceToolManager.getToolObjectsForInstance(toolsetName);
            if (instanceTools != null && !instanceTools.isEmpty()) {
                log.debug("Mapped toolset {} to instance-based tools", toolsetName);
                return instanceTools;
            }

            // Default fallback
            log.warn("Toolset {} not found, using default", toolsetName);
            return getDefaultToolset();
        });
    }

    /**
     * Get default toolset (fallback when toolset is not specified or not found).
     *
     * @return Default tool objects
     */
    private List<Object> getDefaultToolset() {
        return instanceToolManager.getToolObjectsForInstance("default");
    }

    /**
     * Normalize toolset name (lowercase, trim).
     *
     * @param toolset Raw toolset name
     * @return Normalized toolset name
     */
    private String normalizeToolsetName(String toolset) {
        return toolset.trim().toLowerCase();
    }

    /**
     * Remove a toolset from cache.
     *
     * @param toolsetName Toolset identifier
     * @return true if toolset was removed, false if not found
     */
    public boolean unregisterToolset(String toolsetName) {
        String normalizedName = normalizeToolsetName(toolsetName);
        List<Object> removed = toolsetCache.remove(normalizedName);
        if (removed != null) {
            log.info("Unregistered toolset: {}", normalizedName);
            return true;
        }
        return false;
    }

    /**
     * Check if a toolset is registered.
     *
     * @param toolsetName Toolset identifier
     * @return true if toolset is registered
     */
    public boolean isToolsetRegistered(String toolsetName) {
        String normalizedName = normalizeToolsetName(toolsetName);
        return toolsetCache.containsKey(normalizedName);
    }

    /**
     * Get all registered toolset names.
     *
     * @return Set of registered toolset names
     */
    public java.util.Set<String> getRegisteredToolsets() {
        return java.util.Set.copyOf(toolsetCache.keySet());
    }

    /**
     * Clear all registered toolsets from cache.
     */
    public void clearAllToolsets() {
        toolsetCache.clear();
        log.info("Cleared all toolsets from cache");
    }

    /**
     * Get toolset cache size.
     *
     * @return Number of cached toolsets
     */
    public int getCacheSize() {
        return toolsetCache.size();
    }
}

