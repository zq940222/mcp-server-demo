package ai.crewplus.mcpserver.service;

import org.springframework.stereotype.Service;

/**
 * Service for managing instance context using ThreadLocal.
 * This service is independent of tool classes to avoid circular dependencies.
 */
@Service
public class InstanceContext {

    // Current instance context (ThreadLocal for request-scoped instance)
    private static final ThreadLocal<String> currentInstance = new ThreadLocal<>();

    /**
     * Set current instance for this request thread.
     *
     * @param instance Instance identifier
     */
    public void setCurrentInstance(String instance) {
        if (instance != null && !instance.trim().isEmpty()) {
            currentInstance.set(instance.trim().toLowerCase());
        } else {
            currentInstance.set("default");
        }
    }

    /**
     * Get current instance for this request thread.
     *
     * @return Current instance identifier
     */
    public String getCurrentInstance() {
        return currentInstance.get();
    }

    /**
     * Clear current instance for this request thread.
     */
    public void clearCurrentInstance() {
        currentInstance.remove();
    }
}

