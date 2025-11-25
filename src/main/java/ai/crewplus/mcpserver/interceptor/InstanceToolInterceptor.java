package ai.crewplus.mcpserver.interceptor;

import ai.crewplus.mcpserver.service.InstanceToolManager;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * WebFilter to handle instance parameter from request URL and set it in ThreadLocal.
 * Works with Spring WebFlux reactive stack.
 * 
 * The instance parameter is stored in ThreadLocal so that ToolCallbackProvider can
 * dynamically provide the correct tools based on the instance.
 */
@Component
@Order(1)
public class InstanceToolInterceptor implements WebFilter {

    private final InstanceToolManager instanceToolManager;

    public InstanceToolInterceptor(InstanceToolManager instanceToolManager) {
        this.instanceToolManager = instanceToolManager;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Get instance parameter from query string
        String instance = exchange.getRequest().getQueryParams().getFirst("instance");
        
        // Also check for instance in path variables (e.g., /mcp/{instance})
        if (instance == null || instance.isEmpty()) {
            String path = exchange.getRequest().getPath().value();
            if (path != null && path.startsWith("/")) {
                String[] pathParts = path.substring(1).split("/");
                if (pathParts.length > 0 && !pathParts[0].isEmpty()) {
                    // Check if first path segment looks like an instance identifier
                    String firstSegment = pathParts[0];
                    if (firstSegment.matches("^[a-zA-Z0-9_-]+$")) {
                        instance = firstSegment;
                    }
                }
            }
        }

        // Set current instance for this request thread
        // This allows ToolCallbackProvider to return the correct tools
        instanceToolManager.setCurrentInstance(instance);

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    // Clear instance after request completes
                    instanceToolManager.clearCurrentInstance();
                });
    }
}

