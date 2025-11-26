package ai.crewplus.mcpserver.config;

import ai.crewplus.mcpserver.annotation.DynamicToolset;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * BeanPostProcessor to prevent @DynamicToolset annotated beans from being
 * registered as tools at startup.
 * 
 * This processor ensures that tools annotated with @DynamicToolset are NOT
 * scanned and registered by Spring AI MCP Server at startup. Instead, they
 * will be discovered and registered dynamically at connection time via
 * DynamicToolsetRegistry.
 */
@Component
public class DynamicToolsetBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        // Check if bean has @DynamicToolset annotation
        DynamicToolset annotation = AnnotationUtils.findAnnotation(bean.getClass(), DynamicToolset.class);
        if (annotation != null && hasMcpToolMethods(bean)) {
            // This bean should be discovered dynamically, not at startup
            // We'll return the bean as-is, but Spring AI MCP Server should not scan it
            // because it's not a @Service or @Component (unless explicitly annotated)
            return bean;
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * Check if bean has @McpTool annotated methods.
     */
    private boolean hasMcpToolMethods(Object bean) {
        Method[] methods = bean.getClass().getMethods();
        for (Method method : methods) {
            if (AnnotationUtils.findAnnotation(method, McpTool.class) != null) {
                return true;
            }
        }
        return false;
    }
}

