package ai.crewplus.mcpserver.hub;

import java.util.Collections;
import java.util.List;

/**
 * Represents a single MCP instance with its own toolset.
 * 
 * Each instance is independent and contains:
 * - Unique identifier (toolset name)
 * - Display name and description
 * - List of tool objects
 * - Metadata (created time, last accessed, etc.)
 */
public class McpInstance {

    private final String id;
    private final String name;
    private final String description;
    private final List<Object> tools;
    private final long createdAt;
    private long lastAccessedAt;

    public McpInstance(String id, String name, String description, List<Object> tools) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.tools = tools != null ? Collections.unmodifiableList(tools) : Collections.emptyList();
        this.createdAt = System.currentTimeMillis();
        this.lastAccessedAt = this.createdAt;
    }

    /**
     * Get instance identifier (toolset name).
     */
    public String getId() {
        return id;
    }

    /**
     * Get display name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get tool objects for this instance.
     */
    public List<Object> getTools() {
        updateLastAccessed();
        return tools;
    }

    /**
     * Get tool count.
     */
    public int getToolCount() {
        return tools.size();
    }

    /**
     * Check if instance has tools.
     */
    public boolean hasTools() {
        return !tools.isEmpty();
    }

    /**
     * Get creation timestamp.
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Get last accessed timestamp.
     */
    public long getLastAccessedAt() {
        return lastAccessedAt;
    }

    /**
     * Update last accessed timestamp.
     */
    private void updateLastAccessed() {
        this.lastAccessedAt = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return String.format("McpInstance{id='%s', name='%s', tools=%d}", 
                id, name, tools.size());
    }
}

