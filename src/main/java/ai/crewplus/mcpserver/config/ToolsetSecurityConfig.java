package ai.crewplus.mcpserver.config;

import ai.crewplus.mcpserver.service.DynamicToolsetLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.List;

/**
 * Configuration for toolset security and whitelist management.
 * Provides security controls for dynamic toolset loading.
 * 
 * This configuration ensures that only allowed toolsets can be loaded,
 * preventing arbitrary class loading and potential security vulnerabilities.
 */
@Configuration
public class ToolsetSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(ToolsetSecurityConfig.class);

    private final DynamicToolsetLoader toolsetLoader;
    private final List<String> allowedToolsets;

    public ToolsetSecurityConfig(DynamicToolsetLoader toolsetLoader,
                                @Value("${mcp.toolset.allowed:example-tools,example2-tools,order-tools,weather-tools,payment-tools}") 
                                List<String> allowedToolsets) {
        this.toolsetLoader = toolsetLoader;
        this.allowedToolsets = allowedToolsets != null ? allowedToolsets : List.of();
    }

    /**
     * Log security configuration on application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logSecurityConfig() {
        if (allowedToolsets.isEmpty()) {
            log.warn("⚠️  SECURITY WARNING: No toolset whitelist configured. All toolsets will be allowed.");
            log.warn("⚠️  This is not recommended for production environments.");
        } else {
            log.info("✅ Toolset security enabled. Allowed toolsets: {}", allowedToolsets);
        }
    }

    /**
     * Get allowed toolsets list.
     *
     * @return List of allowed toolset names
     */
    public List<String> getAllowedToolsets() {
        return List.copyOf(allowedToolsets);
    }

    /**
     * Check if toolset is allowed.
     *
     * @param toolsetName Toolset identifier
     * @return true if toolset is allowed
     */
    public boolean isToolsetAllowed(String toolsetName) {
        return toolsetLoader.isToolsetAllowed(toolsetName);
    }
}

