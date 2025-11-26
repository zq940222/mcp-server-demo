package ai.crewplus.mcpserver.config;

import org.springframework.context.annotation.Configuration;

/**
 * MCP Server configuration for dynamic tool registration.
 * 
 * IMPORTANT: This configuration does NOT create any tool beans.
 * 
 * Tools annotated with @DynamicToolset are discovered and registered
 * dynamically at connection time via DynamicToolsetRegistry.
 * 
 * This enables true dynamic discovery:
 * - Tools are NOT registered at startup
 * - Tools are discovered and registered at connection time
 * - Only tools matching the toolset parameter are registered
 */
@Configuration
public class McpServerConfig {
    // No bean definitions here - tools are discovered dynamically via @DynamicToolset annotation
    // DynamicToolsetRegistry handles dynamic discovery and registration at connection time
}
