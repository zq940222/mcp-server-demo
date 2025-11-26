package ai.crewplus.mcpserver.registry;

import ai.crewplus.mcpserver.annotation.DynamicToolset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing dynamic toolsets.
 * 
 * This registry tracks tool classes annotated with @DynamicToolset and
 * dynamically creates and registers them as Spring beans at connection time
 * based on the toolset parameter from the request.
 */
@Component
public class DynamicToolsetRegistry implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(DynamicToolsetRegistry.class);

    private ConfigurableApplicationContext applicationContext;
    
    // Map of toolset name -> Set of tool class types
    private final Map<String, Set<Class<?>>> toolsetClasses = new ConcurrentHashMap<>();
    
    // Map of toolset name -> Set of registered bean names (for cleanup)
    private final Map<String, Set<String>> registeredBeans = new ConcurrentHashMap<>();
    
    // Cache of toolset -> tool instances (for performance)
    private final Map<String, List<Object>> toolsetInstances = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }
    
    @PostConstruct
    public void init() {
        scanForDynamicToolsets();
    }

    /**
     * Scan the application context for classes annotated with @DynamicToolset.
     */
    private void scanForDynamicToolsets() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
        
        for (String beanName : beanNames) {
            try {
                Class<?> beanType = beanFactory.getType(beanName);
                if (beanType != null) {
                    DynamicToolset annotation = AnnotationUtils.findAnnotation(beanType, DynamicToolset.class);
                    if (annotation != null) {
                        registerToolsetClass(annotation, beanType);
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to check bean {} for @DynamicToolset: {}", beanName, e.getMessage());
            }
        }
        
        // Also scan package for annotated classes
        scanPackageForDynamicToolsets("ai.crewplus.mcpserver.tool");
    }

    /**
     * Scan a package for classes annotated with @DynamicToolset.
     */
    private void scanPackageForDynamicToolsets(String packageName) {
        try {
            // Use Spring's ClassPathScanningCandidateComponentProvider for better compatibility
            org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider scanner =
                    new org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider(false);
            
            scanner.addIncludeFilter(new org.springframework.core.type.filter.AnnotationTypeFilter(DynamicToolset.class));
            
            java.util.Set<org.springframework.beans.factory.config.BeanDefinition> candidates = 
                    scanner.findCandidateComponents(packageName);
            
            for (org.springframework.beans.factory.config.BeanDefinition candidate : candidates) {
                try {
                    Class<?> clazz = Class.forName(candidate.getBeanClassName());
                    DynamicToolset annotation = AnnotationUtils.findAnnotation(clazz, DynamicToolset.class);
                    if (annotation != null) {
                        registerToolsetClass(annotation, clazz);
                    }
                } catch (ClassNotFoundException e) {
                    log.debug("Failed to load class {}: {}", candidate.getBeanClassName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("Failed to scan package {} for dynamic toolsets: {}", packageName, e.getMessage());
        }
    }


    /**
     * Register a toolset class.
     */
    private void registerToolsetClass(DynamicToolset annotation, Class<?> toolClass) {
        String[] toolsets = annotation.value();
        for (String toolset : toolsets) {
            String normalizedToolset = toolset.trim().toLowerCase();
            toolsetClasses.computeIfAbsent(normalizedToolset, k -> new HashSet<>()).add(toolClass);
        }
    }

    /**
     * Get tool instances for a specific toolset.
     * Creates and registers tools dynamically if not already registered.
     * 
     * @param toolset Toolset identifier
     * @return List of tool instances for this toolset
     */
    public List<Object> getToolsForToolset(String toolset) {
        if (toolset == null || toolset.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String normalizedToolset = toolset.trim().toLowerCase();
        
        // Check cache first
        List<Object> cached = toolsetInstances.get(normalizedToolset);
        if (cached != null) {
            return cached;
        }
        
        // Get tool classes for this toolset
        Set<Class<?>> toolClasses = toolsetClasses.get(normalizedToolset);
        if (toolClasses == null || toolClasses.isEmpty()) {
            log.warn("No tools found for toolset: {}", normalizedToolset);
            return Collections.emptyList();
        }
        
        // Create tool instances dynamically
        List<Object> instances = new ArrayList<>();
        Set<String> beanNames = new HashSet<>();
        
        for (Class<?> toolClass : toolClasses) {
            try {
                Object instance = createToolInstance(toolClass);
                if (instance != null) {
                    instances.add(instance);
                    
                    // Register as Spring bean for this request
                    String beanName = generateBeanName(normalizedToolset, toolClass);
                    registerToolBean(beanName, instance, toolClass);
                    beanNames.add(beanName);
                }
            } catch (Exception e) {
                log.error("Failed to create tool instance for {}: {}", toolClass.getName(), e.getMessage(), e);
            }
        }
        
        // Cache instances
        toolsetInstances.put(normalizedToolset, instances);
        registeredBeans.put(normalizedToolset, beanNames);
        
        return instances;
    }

    /**
     * Create a tool instance.
     */
    private Object createToolInstance(Class<?> toolClass) throws Exception {
        Object instance = toolClass.getDeclaredConstructor().newInstance();
        
        // Try to inject InstanceContext if method exists
        try {
            java.lang.reflect.Method setContextMethod = toolClass.getMethod("setInstanceContext", 
                    ai.crewplus.mcpserver.service.InstanceContext.class);
            ai.crewplus.mcpserver.service.InstanceContext instanceContext = 
                    applicationContext.getBean(ai.crewplus.mcpserver.service.InstanceContext.class);
            setContextMethod.invoke(instance, instanceContext);
        } catch (NoSuchMethodException e) {
            // Method doesn't exist, that's okay
        }
        
        return instance;
    }

    /**
     * Register a tool instance as a Spring bean.
     * Uses BeanDefinition to ensure Spring AI MCP Server can discover it.
     */
    private void registerToolBean(String beanName, Object instance, Class<?> toolClass) {
        ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
        
        // Check if bean already exists
        if (beanFactory.containsSingleton(beanName) || 
            beanFactory.containsBean(beanName)) {
            log.debug("Bean {} already exists, skipping registration", beanName);
            return;
        }
        
        try {
            // Use DefaultListableBeanFactory to register BeanDefinition
            if (beanFactory instanceof org.springframework.beans.factory.support.DefaultListableBeanFactory) {
                org.springframework.beans.factory.support.DefaultListableBeanFactory defaultBeanFactory = 
                    (org.springframework.beans.factory.support.DefaultListableBeanFactory) beanFactory;
                
                // Create BeanDefinition
                org.springframework.beans.factory.support.GenericBeanDefinition beanDefinition = 
                    new org.springframework.beans.factory.support.GenericBeanDefinition();
                beanDefinition.setBeanClass(toolClass);
                beanDefinition.setScope(org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON);
                beanDefinition.setAutowireMode(org.springframework.beans.factory.config.AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE);
                
                // Register BeanDefinition first
                defaultBeanFactory.registerBeanDefinition(beanName, beanDefinition);
                
                // Then register singleton instance
                beanFactory.registerSingleton(beanName, instance);
                
                // Also register with standard naming convention (first letter lowercase)
                String standardBeanName = Character.toLowerCase(toolClass.getSimpleName().charAt(0)) + 
                                         toolClass.getSimpleName().substring(1);
                if (!standardBeanName.equals(beanName) && !beanFactory.containsBean(standardBeanName)) {
                    defaultBeanFactory.registerBeanDefinition(standardBeanName, beanDefinition);
                    beanFactory.registerSingleton(standardBeanName, instance);
                }
            } else {
                // Fallback: just register singleton
                beanFactory.registerSingleton(beanName, instance);
            }
        } catch (Exception e) {
            log.error("Failed to register tool bean {}: {}", beanName, e.getMessage(), e);
            // Fallback: try simple singleton registration
            try {
                beanFactory.registerSingleton(beanName, instance);
            } catch (Exception e2) {
                log.error("Fallback registration also failed: {}", e2.getMessage());
            }
        }
    }

    /**
     * Generate a unique bean name for a tool.
     */
    private String generateBeanName(String toolset, Class<?> toolClass) {
        return toolset + "-" + toolClass.getSimpleName().toLowerCase();
    }

    /**
     * Clear cached instances for a toolset (for cleanup).
     */
    public void clearToolsetCache(String toolset) {
        String normalizedToolset = toolset != null ? toolset.trim().toLowerCase() : null;
        if (normalizedToolset != null) {
            toolsetInstances.remove(normalizedToolset);
            registeredBeans.remove(normalizedToolset);
        }
    }

    /**
     * Get all registered toolsets.
     * 
     * @return Set of toolset identifiers
     */
    public Set<String> getRegisteredToolsets() {
        return Collections.unmodifiableSet(toolsetClasses.keySet());
    }

    /**
     * Check if a toolset is registered.
     * 
     * @param toolset Toolset identifier
     * @return true if toolset is registered
     */
    public boolean isToolsetRegistered(String toolset) {
        String normalizedToolset = toolset != null ? toolset.trim().toLowerCase() : null;
        return normalizedToolset != null && toolsetClasses.containsKey(normalizedToolset);
    }
}

