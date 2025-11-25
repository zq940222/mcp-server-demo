package ai.crewplus.mcpserver.config;

import ai.crewplus.mcpserver.service.InstanceContext;
import ai.crewplus.mcpserver.tool.ExampleTools;
import ai.crewplus.mcpserver.tool.Example2Tools;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Server configuration for dynamic tool registration.
 * Tools will be registered dynamically based on instance parameter from the connection URL.
 * 
 * Since Spring AI MCP Server automatically scans @McpTool annotations from @Service beans,
 * we manually create tool beans here and control which ones are registered based on instance.
 * 
 * However, Spring AI MCP Server still scans all @McpTool methods, so we need to use
 * a different approach: create tool beans conditionally or use a custom tool provider.
 */
@Configuration
public class McpServerConfig {

    private final InstanceContext instanceContext;

    public McpServerConfig(InstanceContext instanceContext) {
        this.instanceContext = instanceContext;
    }

    /**
     * Create ExampleTools bean.
     * This bean will be scanned by Spring AI MCP Server for @McpTool annotations.
     * Tools will check instance parameter at runtime.
     */
    @Bean
    public ExampleTools exampleTools() {
        ExampleTools tools = new ExampleTools();
        tools.setInstanceContext(instanceContext);
        return tools;
    }

    /**
     * Create Example2Tools bean.
     * This bean will be scanned by Spring AI MCP Server for @McpTool annotations.
     * Tools will check instance parameter at runtime.
     */
    @Bean
    public Example2Tools example2Tools() {
        Example2Tools tools = new Example2Tools();
        tools.setInstanceContext(instanceContext);
        return tools;
    }
}
