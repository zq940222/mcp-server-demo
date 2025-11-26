package ai.crewplus.mcpserver.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to disable Spring AI MCP Server auto-configuration
 * when using custom MCP Server.
 * 
 * Set property: mcp.server.custom.enabled=true to use custom MCP Server
 */
@Configuration
@ConditionalOnProperty(name = "mcp.server.custom.enabled", havingValue = "true", matchIfMissing = false)
public class CustomMcpServerConfig {
    
    // This configuration can be used to disable Spring AI MCP Server
    // if needed in the future
}

