package ai.crewplus.mcpserver.hub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-MCP Hub: Centralized manager for multiple MCP instances.
 * 
 * Each instance represents a separate toolset that can be dynamically loaded
 * and managed independently. The Hub routes requests to the appropriate instance
 * based on the toolset parameter.
 * 
 * Architecture:
 * - McpHub: Central manager
 * - McpInstance: Each instance contains a toolset with its own tools
 * - InstanceRouter: Routes requests to the correct instance
 * - DynamicInstanceLoader: Loads and creates instances dynamically
 */
@Component
public class McpHub {

    private static final Logger log = LoggerFactory.getLogger(McpHub.class);

    private final Map<String, McpInstance> instances = new ConcurrentHashMap<>();
    private final InstanceLoader instanceLoader;

    @Autowired
    public McpHub(InstanceLoader instanceLoader) {
        this.instanceLoader = instanceLoader;
    }

    /**
     * Get or create an MCP instance for the given toolset.
     * If the instance doesn't exist, it will be dynamically loaded.
     * 
     * @param toolset Toolset identifier
     * @return McpInstance for the toolset
     */
    public McpInstance getInstance(String toolset) {
        if (toolset == null || toolset.isEmpty()) {
            log.debug("No toolset specified, returning default instance");
            return getDefaultInstance();
        }

        // Check if instance already exists
        McpInstance instance = instances.get(toolset);
        if (instance != null) {
            log.info("✅ Found existing cached instance: {} ({} tools)", toolset, instance.getToolCount());
            return instance;
        }

        // Try to load instance dynamically
        log.info("🔧 Loading instance dynamically for toolset: {}", toolset);
        instance = instanceLoader.loadInstance(toolset);
        
        if (instance != null) {
            instances.put(toolset, instance);
            log.info("✅ Successfully loaded and cached instance: {} ({} tools)", 
                    toolset, instance.getToolCount());
            return instance;
        }

        log.warn("⚠️ Could not load instance for toolset: {}, returning default", toolset);
        return getDefaultInstance();
    }

    /**
     * Get the default instance (empty or default toolset).
     */
    public McpInstance getDefaultInstance() {
        return instances.computeIfAbsent("default", key -> {
            log.info("Creating default instance");
            return new McpInstance("default", "Default Instance", 
                    "Default instance with no tools", Collections.emptyList());
        });
    }

    /**
     * Register an instance manually.
     */
    public void registerInstance(McpInstance instance) {
        instances.put(instance.getId(), instance);
        log.info("✅ Registered instance: {} ({} tools)", 
                instance.getId(), instance.getToolCount());
    }

    /**
     * Get all registered instances.
     */
    public Collection<McpInstance> getAllInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    /**
     * Get instance by ID.
     */
    public McpInstance getInstanceById(String instanceId) {
        return instances.get(instanceId);
    }

    /**
     * Remove an instance.
     */
    public void removeInstance(String toolset) {
        McpInstance removed = instances.remove(toolset);
        if (removed != null) {
            log.info("✅ Removed instance: {}", toolset);
        }
    }

    /**
     * Clear all instances (useful for testing or reset).
     */
    public void clearAllInstances() {
        instances.clear();
        log.info("✅ Cleared all instances");
    }

    /**
     * Get instance count.
     */
    public int getInstanceCount() {
        return instances.size();
    }
}

