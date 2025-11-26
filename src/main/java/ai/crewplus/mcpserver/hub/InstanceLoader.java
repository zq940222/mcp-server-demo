package ai.crewplus.mcpserver.hub;

import ai.crewplus.mcpserver.annotation.DynamicToolset;
import ai.crewplus.mcpserver.registry.DynamicToolsetRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads MCP instances dynamically based on @DynamicToolset annotations.
 * 
 * Scans the classpath for classes annotated with @DynamicToolset and creates
 * McpInstance objects for each toolset.
 */
@Component
public class InstanceLoader {

    private static final Logger log = LoggerFactory.getLogger(InstanceLoader.class);

    private final ApplicationContext applicationContext;
    private final DynamicToolsetRegistry dynamicToolsetRegistry;
    private final Map<String, InstanceMetadata> instanceMetadataCache = new ConcurrentHashMap<>();

    @Autowired
    public InstanceLoader(ApplicationContext applicationContext,
                         DynamicToolsetRegistry dynamicToolsetRegistry) {
        this.applicationContext = applicationContext;
        this.dynamicToolsetRegistry = dynamicToolsetRegistry;
        initializeMetadataCache();
    }

    /**
     * Initialize metadata cache by scanning for @DynamicToolset annotations.
     */
    private void initializeMetadataCache() {
        // Get all beans and check for @DynamicToolset annotation
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            try {
                Class<?> beanClass = applicationContext.getType(beanName);
                if (beanClass != null) {
                    DynamicToolset annotation = AnnotationUtils.findAnnotation(beanClass, DynamicToolset.class);
                    if (annotation != null) {
                        registerInstanceMetadata(annotation, beanClass);
                    }
                }
            } catch (Exception e) {
                log.debug("Could not check bean {} for @DynamicToolset: {}", beanName, e.getMessage());
            }
        }

        // Also scan package for classes (in case they're not Spring beans)
        scanPackageForDynamicToolsets();
    }

    /**
     * Register instance metadata from @DynamicToolset annotation.
     */
    private void registerInstanceMetadata(DynamicToolset annotation, Class<?> toolClass) {
        String[] toolsetIds = annotation.value();
        String name = annotation.name().isEmpty() ? toolClass.getSimpleName() : annotation.name();
        String description = annotation.description().isEmpty() 
                ? "Tools from " + toolClass.getSimpleName() 
                : annotation.description();

        for (String toolsetId : toolsetIds) {
            InstanceMetadata metadata = new InstanceMetadata(toolsetId, name, description, toolClass);
            instanceMetadataCache.put(toolsetId, metadata);
        }
    }

    /**
     * Scan package for classes with @DynamicToolset annotation.
     */
    private void scanPackageForDynamicToolsets() {
        try {
            // Use Spring's ClassPathScanningCandidateComponentProvider to scan package
            org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider scanner =
                    new org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider(false);
            
            scanner.addIncludeFilter(new org.springframework.core.type.filter.AnnotationTypeFilter(DynamicToolset.class));
            
            // Scan the tool package
            String packageName = "ai.crewplus.mcpserver.tool";
            java.util.Set<org.springframework.beans.factory.config.BeanDefinition> candidates = 
                    scanner.findCandidateComponents(packageName);
            
            for (org.springframework.beans.factory.config.BeanDefinition candidate : candidates) {
                try {
                    Class<?> clazz = Class.forName(candidate.getBeanClassName());
                    DynamicToolset annotation = AnnotationUtils.findAnnotation(clazz, DynamicToolset.class);
                    if (annotation != null) {
                        registerInstanceMetadata(annotation, clazz);
                    }
                } catch (ClassNotFoundException e) {
                    log.debug("Failed to load class {}: {}", candidate.getBeanClassName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("Could not scan package for @DynamicToolset: {}", e.getMessage());
        }
    }

    /**
     * Load an instance for the given toolset.
     * 
     * @param toolset Toolset identifier
     * @return McpInstance or null if not found
     */
    public McpInstance loadInstance(String toolset) {
        try {
            // Get metadata for this toolset
            InstanceMetadata metadata = instanceMetadataCache.get(toolset);
            if (metadata == null) {
                log.warn("No metadata found for toolset: {}", toolset);
                return null;
            }

            // Load tools for this toolset
            List<Object> tools = dynamicToolsetRegistry.getToolsForToolset(toolset);
            
            if (tools.isEmpty()) {
                log.warn("No tools found for toolset: {}", toolset);
                return null;
            }

            // Create instance
            return new McpInstance(
                    toolset,
                    metadata.getName(),
                    metadata.getDescription(),
                    tools
            );

        } catch (Exception e) {
            log.error("Failed to load instance for toolset {}: {}", toolset, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get metadata for a toolset.
     */
    public InstanceMetadata getMetadata(String toolset) {
        return instanceMetadataCache.get(toolset);
    }

    /**
     * Get all available toolsets.
     */
    public java.util.Set<String> getAvailableToolsets() {
        return instanceMetadataCache.keySet();
    }

    /**
     * Metadata for an instance (from @DynamicToolset annotation).
     */
    public static class InstanceMetadata {
        private final String id;
        private final String name;
        private final String description;
        private final Class<?> toolClass;

        public InstanceMetadata(String id, String name, String description, Class<?> toolClass) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.toolClass = toolClass;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Class<?> getToolClass() { return toolClass; }
    }
}

