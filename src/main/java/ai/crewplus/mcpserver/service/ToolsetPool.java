package ai.crewplus.mcpserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Toolset pool for caching frequently used toolsets.
 * Provides thread-safe caching with expiration and size limits.
 * 
 * This component optimizes performance by caching toolset instances,
 * reducing the overhead of dynamic loading for frequently accessed toolsets.
 */
@Component
public class ToolsetPool {

    private static final Logger log = LoggerFactory.getLogger(ToolsetPool.class);

    // Simple cache implementation (can be replaced with Caffeine if available)
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    
    private final int maxSize;
    private final Duration expireAfterWrite;
    private final ReentrantLock lock = new ReentrantLock();

    public ToolsetPool(@Value("${mcp.toolset.cache.max-size:10}") int maxSize,
                      @Value("${mcp.toolset.cache.expire-minutes:30}") long expireMinutes) {
        this.maxSize = maxSize;
        this.expireAfterWrite = Duration.ofMinutes(expireMinutes);
        log.info("Initialized ToolsetPool with maxSize={}, expireAfterWrite={}", maxSize, expireAfterWrite);
    }

    /**
     * Get toolset from cache or load it.
     *
     * @param toolsetName Toolset identifier
     * @param loader Function to load toolset if not in cache
     * @return List of tool objects for this toolset
     */
    public List<Object> getOrLoad(String toolsetName, java.util.function.Function<String, List<Object>> loader) {
        String normalizedName = normalizeToolsetName(toolsetName);
        
        // Check cache first
        CacheEntry entry = cache.get(normalizedName);
        if (entry != null && !entry.isExpired()) {
            log.debug("Cache hit for toolset: {}", normalizedName);
            return entry.getToolObjects();
        }

        // Cache miss or expired, load toolset
        lock.lock();
        try {
            // Double-check after acquiring lock
            entry = cache.get(normalizedName);
            if (entry != null && !entry.isExpired()) {
                log.debug("Cache hit after lock for toolset: {}", normalizedName);
                return entry.getToolObjects();
            }

            // Load toolset
            List<Object> toolObjects = loader.apply(normalizedName);
            if (toolObjects == null || toolObjects.isEmpty()) {
                log.warn("Failed to load toolset: {}", normalizedName);
                return toolObjects;
            }

            // Check cache size and evict if necessary
            evictIfNecessary();

            // Put in cache
            cache.put(normalizedName, new CacheEntry(toolObjects, System.currentTimeMillis()));
            log.info("Cached toolset: {} (cache size: {})", normalizedName, cache.size());
            
            return toolObjects;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Put toolset in cache.
     *
     * @param toolsetName Toolset identifier
     * @param toolObjects List of tool objects
     */
    public void put(String toolsetName, List<Object> toolObjects) {
        String normalizedName = normalizeToolsetName(toolsetName);
        
        lock.lock();
        try {
            evictIfNecessary();
            cache.put(normalizedName, new CacheEntry(toolObjects, System.currentTimeMillis()));
            log.debug("Put toolset in cache: {}", normalizedName);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get toolset from cache (without loading).
     *
     * @param toolsetName Toolset identifier
     * @return List of tool objects or null if not in cache or expired
     */
    public List<Object> get(String toolsetName) {
        String normalizedName = normalizeToolsetName(toolsetName);
        CacheEntry entry = cache.get(normalizedName);
        
        if (entry == null) {
            return null;
        }
        
        if (entry.isExpired()) {
            cache.remove(normalizedName);
            return null;
        }
        
        return entry.getToolObjects();
    }

    /**
     * Remove toolset from cache.
     *
     * @param toolsetName Toolset identifier
     * @return true if toolset was removed
     */
    public boolean evict(String toolsetName) {
        String normalizedName = normalizeToolsetName(toolsetName);
        CacheEntry removed = cache.remove(normalizedName);
        if (removed != null) {
            log.debug("Evicted toolset from cache: {}", normalizedName);
            return true;
        }
        return false;
    }

    /**
     * Clear all cached toolsets.
     */
    public void clear() {
        lock.lock();
        try {
            int size = cache.size();
            cache.clear();
            log.info("Cleared {} toolsets from cache", size);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get cache size.
     *
     * @return Number of cached toolsets
     */
    public int size() {
        return cache.size();
    }

    /**
     * Evict expired entries and enforce size limit.
     */
    private void evictIfNecessary() {
        // Remove expired entries
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        // If still over limit, remove oldest entries
        if (cache.size() >= maxSize) {
            long oldestTime = Long.MAX_VALUE;
            String oldestKey = null;
            
            for (java.util.Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
                long timestamp = entry.getValue().getTimestamp();
                if (timestamp < oldestTime) {
                    oldestTime = timestamp;
                    oldestKey = entry.getKey();
                }
            }
            
            if (oldestKey != null) {
                cache.remove(oldestKey);
                log.debug("Evicted oldest toolset from cache: {}", oldestKey);
            }
        }
    }

    /**
     * Normalize toolset name.
     *
     * @param toolsetName Raw toolset name
     * @return Normalized toolset name
     */
    private String normalizeToolsetName(String toolsetName) {
        return toolsetName.trim().toLowerCase();
    }

    /**
     * Cache entry with expiration timestamp.
     */
    private class CacheEntry {
        private final List<Object> toolObjects;
        private final long timestamp;

        public CacheEntry(List<Object> toolObjects, long timestamp) {
            this.toolObjects = toolObjects;
            this.timestamp = timestamp;
        }

        public List<Object> getToolObjects() {
            return toolObjects;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > expireAfterWrite.toMillis();
        }
    }
}

