package ai.crewplus.mcpserver.interceptor;

import ai.crewplus.mcpserver.service.InstanceToolManager;
import ai.crewplus.mcpserver.service.ToolsetRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * WebFilter to handle toolset parameter from request (Header, body, or query param) and set it in ThreadLocal.
 * Works with Spring WebFlux reactive stack.
 * 
 * Supports multiple parameter sources:
 * 1. HTTP Header: X-Toolset
 * 2. Query parameter: ?toolset=xxx
 * 3. Request body field: {"toolset": "xxx"}
 * 4. Legacy instance parameter: ?instance=xxx (for backward compatibility)
 * 
 * The toolset parameter is stored in ThreadLocal so that ToolCallbackProvider can
 * dynamically provide the correct tools based on the toolset.
 */
@Component
@Order(1)
public class InstanceToolInterceptor implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(InstanceToolInterceptor.class);
    
    private static final String HEADER_TOOLSET = "X-Toolset";
    private static final String QUERY_PARAM_TOOLSET = "toolset";
    private static final String QUERY_PARAM_INSTANCE = "instance"; // Legacy support

    private final InstanceToolManager instanceToolManager;
    private final ToolsetRouter toolsetRouter;

    public InstanceToolInterceptor(InstanceToolManager instanceToolManager,
                                  ToolsetRouter toolsetRouter) {
        this.instanceToolManager = instanceToolManager;
        this.toolsetRouter = toolsetRouter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Priority 1: Check HTTP Header (X-Toolset)
        String toolset = exchange.getRequest().getHeaders().getFirst(HEADER_TOOLSET);
        
        // Priority 2: Check query parameter (toolset)
        if (toolset == null || toolset.isEmpty()) {
            toolset = exchange.getRequest().getQueryParams().getFirst(QUERY_PARAM_TOOLSET);
        }
        
        // Priority 3: Check legacy instance parameter (for backward compatibility)
        if (toolset == null || toolset.isEmpty()) {
            toolset = exchange.getRequest().getQueryParams().getFirst(QUERY_PARAM_INSTANCE);
        }
        
        // Priority 4: Check path variables (e.g., /mcp/{toolset})
        if (toolset == null || toolset.isEmpty()) {
            String path = exchange.getRequest().getPath().value();
            if (path != null && path.startsWith("/")) {
                String[] pathParts = path.substring(1).split("/");
                if (pathParts.length > 0 && !pathParts[0].isEmpty()) {
                    String firstSegment = pathParts[0];
                    if (firstSegment.matches("^[a-zA-Z0-9_-]+$")) {
                        toolset = firstSegment;
                    }
                }
            }
        }
        
        // Priority 5: Try to extract from request body (for POST requests)
        // Note: In WebFlux, request body can only be read once.
        // For body-based toolset extraction, we'll use a wrapper approach.
        // For now, we'll skip body parsing in the filter to avoid complexity.
        // Body parsing can be handled in the controller if needed.
        
        // Set toolset/instance for this request thread
        setToolsetForRequest(toolset);
        
        // Pre-load toolset to cache (optimization)
        if (toolset != null && !toolset.isEmpty()) {
            try {
                toolsetRouter.getToolObjectsForToolset(toolset);
            } catch (Exception e) {
                log.debug("Failed to pre-load toolset {}: {}", toolset, e.getMessage());
            }
        }

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    // Clear toolset/instance after request completes
                    instanceToolManager.clearCurrentInstance();
                });
    }


    /**
     * Set toolset/instance for current request thread.
     *
     * @param toolset Toolset identifier
     */
    private void setToolsetForRequest(String toolset) {
        if (toolset != null && !toolset.isEmpty()) {
            log.debug("Setting toolset for request: {}", toolset);
            // Use instance parameter for backward compatibility
            instanceToolManager.setCurrentInstance(toolset);
        } else {
            log.debug("No toolset specified, using default");
        }
    }
}

