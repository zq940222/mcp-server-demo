package ai.crewplus.mcpserver.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark tool classes for dynamic toolset discovery.
 * 
 * Tools annotated with @DynamicToolset will NOT be registered at startup.
 * Instead, they will be discovered and registered dynamically at connection time
 * based on the toolset parameter from the request.
 * 
 * Usage:
 * <pre>
 * {@code
 * @DynamicToolset("example-tools")
 * public class ExampleTools {
 *     @McpTool(description = "...")
 *     public String calculator(...) { ... }
 * }
 * }
 * </pre>
 * 
 * @author Dynamic Toolset Framework
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DynamicToolset {
    
    /**
     * Toolset identifier(s) that this tool class belongs to.
     * Can specify multiple toolsets separated by comma.
     * 
     * @return Array of toolset identifiers
     */
    String[] value();
    
    /**
     * Optional: Toolset name for display purposes.
     * 
     * @return Toolset display name
     */
    String name() default "";
    
    /**
     * Optional: Description of this toolset.
     * 
     * @return Toolset description
     */
    String description() default "";
    
    /**
     * Optional: Whether this toolset is enabled by default.
     * If false, toolset must be explicitly requested via toolset parameter.
     * 
     * @return true if enabled by default
     */
    boolean enabledByDefault() default false;
}

