package ai.crewplus.mcpserver.service;

import ai.crewplus.mcpserver.tool.ExampleTools;
import ai.crewplus.mcpserver.tool.Example2Tools;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing tool registration based on instance parameter.
 * Dynamically provides different tool objects based on the instance identifier from the connection URL.
 * 
 * This service works with Spring AI MCP Server by providing tool objects that should be registered
 * based on the instance parameter from the request.
 */
@Service
public class InstanceToolManager {

    private final ExampleTools exampleTools;
    private final Example2Tools example2Tools;
    private final InstanceContext instanceContext;

    public InstanceToolManager(ExampleTools exampleTools, 
                              Example2Tools example2Tools,
                              InstanceContext instanceContext) {
        this.exampleTools = exampleTools;
        this.example2Tools = example2Tools;
        this.instanceContext = instanceContext;
    }

    /**
     * Set current instance for this request thread.
     *
     * @param instance Instance identifier
     */
    public void setCurrentInstance(String instance) {
        instanceContext.setCurrentInstance(instance);
    }

    /**
     * Get current instance for this request thread.
     *
     * @return Current instance identifier
     */
    public String getCurrentInstance() {
        return instanceContext.getCurrentInstance();
    }

    /**
     * Clear current instance for this request thread.
     */
    public void clearCurrentInstance() {
        instanceContext.clearCurrentInstance();
    }

    /**
     * Get tool objects for a specific instance.
     * Based on the instance parameter, different sets of tools will be returned.
     *
     * @param instance Instance identifier from the connection URL
     * @return List of tool objects for this instance
     */
    public List<Object> getToolObjectsForInstance(String instance) {
        if (instance == null || instance.trim().isEmpty()) {
            instance = "default";
        }

        String instanceKey = instance.trim().toLowerCase();
        List<Object> toolObjects = new ArrayList<>();

        switch (instanceKey) {
            case "example1":
            case "instance1":
                // Return ExampleTools only
                toolObjects.add(exampleTools);
                break;

            case "example2":
            case "instance2":
                // Return Example2Tools only
                toolObjects.add(example2Tools);
                break;

            case "all":
            case "both":
                // Return both tool sets
                toolObjects.add(exampleTools);
                toolObjects.add(example2Tools);
                break;

            default:
                // For unknown instances, return default tools
                toolObjects.add(exampleTools);
                break;
        }

        return toolObjects;
    }

    /**
     * Get tool objects for the current instance (from ThreadLocal).
     *
     * @return List of tool objects for current instance
     */
    public List<Object> getCurrentInstanceToolObjects() {
        String instance = getCurrentInstance();
        if (instance == null) {
            instance = "default";
        }
        return getToolObjectsForInstance(instance);
    }
}
