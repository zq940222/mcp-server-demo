package ai.crewplus.mcpserver.config;

import org.springframework.context.annotation.Configuration;

/**
 * Web configuration.
 * InstanceToolInterceptor is automatically registered as a WebFilter component.
 * No additional configuration needed for WebFlux.
 */
@Configuration
public class WebConfig {
    // WebFilter is automatically registered by Spring Boot
    // InstanceToolInterceptor is annotated with @Component and implements WebFilter
}

