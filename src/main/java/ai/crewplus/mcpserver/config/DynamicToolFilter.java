package ai.crewplus.mcpserver.config;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BeanPostProcessor to track tool beans.
 * This processor tracks which beans have @McpTool methods.
 */
@Component
public class DynamicToolFilter implements BeanPostProcessor {

    private final Set<Object> toolBeans = ConcurrentHashMap.newKeySet();
    
    // Map to track which tools belong to which instance
    private static final Set<String> INSTANCE1_TOOLS = Set.of("calculator", "greeting", "getCurrentTime");
    private static final Set<String> INSTANCE2_TOOLS = Set.of("getWeather", "convertTemperature", "generateRandomNumber", "reverseString");

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        // Check if bean has @McpTool methods
        if (hasMcpToolMethods(bean)) {
            toolBeans.add(bean);
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

    /**
     * Get tools that should be available for the current instance.
     * This method can be called by MCP Server to filter tools.
     */
    public Set<String> getAvailableToolNames(String instance) {
        String instanceKey = (instance == null || instance.trim().isEmpty()) 
                ? "default" 
                : instance.trim().toLowerCase();

        switch (instanceKey) {
            case "example1":
            case "instance1":
                return INSTANCE1_TOOLS;
            case "example2":
            case "instance2":
                return INSTANCE2_TOOLS;
            case "all":
            case "both":
                Set<String> all = new HashSet<>(INSTANCE1_TOOLS);
                all.addAll(INSTANCE2_TOOLS);
                return all;
            default:
                return INSTANCE1_TOOLS;
        }
    }
}

