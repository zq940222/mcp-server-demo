package ai.crewplus.mcpserver.interceptor;

import ai.crewplus.mcpserver.service.InstanceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * WebFilter to extract toolset parameter from requests and set it in ThreadLocal context.
 * 
 * This interceptor:
 * 1. Extracts toolset parameter from request (Header, query param, etc.)
 * 2. Sets toolset in InstanceContext (ThreadLocal) for CustomMcpServer to use
 * 
 * The CustomMcpServer will use this toolset to dynamically load and provide tools.
 */
@Component
@Order(0) // Execute before InstanceToolInterceptor
public class DynamicToolsetInterceptor implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(DynamicToolsetInterceptor.class);
    
    private static final String HEADER_TOOLSET = "X-Toolset";
    private static final String QUERY_PARAM_TOOLSET = "toolset";
    private static final String QUERY_PARAM_INSTANCE = "instance"; // Legacy support

    private final InstanceContext instanceContext;

    @Autowired
    public DynamicToolsetInterceptor(InstanceContext instanceContext) {
        this.instanceContext = instanceContext;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Extract toolset parameter
        String toolset = extractToolset(exchange);
        
        if (toolset != null && !toolset.isEmpty()) {
            log.debug("🔍 Setting toolset in context: {}", toolset);
            // Set toolset in ThreadLocal for CustomMcpServer to use
            instanceContext.setCurrentInstance(toolset);
        } else {
            log.debug("No toolset specified in request");
        }

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    // Cleanup: clear toolset after request completes
                    instanceContext.clearCurrentInstance();
                });
    }

    /**
     * Extract toolset parameter from request.
     * Priority: Header > Query Param > Legacy Instance Param > Path Variable
     */
    private String extractToolset(ServerWebExchange exchange) {
        // Priority 1: HTTP Header
        String toolset = exchange.getRequest().getHeaders().getFirst(HEADER_TOOLSET);
        
        // Priority 2: Query parameter
        if (toolset == null || toolset.isEmpty()) {
            toolset = exchange.getRequest().getQueryParams().getFirst(QUERY_PARAM_TOOLSET);
        }
        
        // Priority 3: Legacy instance parameter
        if (toolset == null || toolset.isEmpty()) {
            toolset = exchange.getRequest().getQueryParams().getFirst(QUERY_PARAM_INSTANCE);
        }
        
        // Priority 4: Path variable
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
        
        return toolset != null ? toolset.trim() : null;
    }
}

