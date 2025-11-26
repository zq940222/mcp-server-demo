package ai.crewplus.mcpserver.service;

import ai.crewplus.mcpserver.tool.ExampleTools;
import ai.crewplus.mcpserver.tool.Example2Tools;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing tool registration based on instance/toolset parameter.
 * Dynamically provides different tool objects based on the instance/toolset identifier from the connection URL or request.
 * 
 * This service works with Spring AI MCP Server by providing tool objects that should be registered
 * based on the instance/toolset parameter from the request.
 * 
 * Supports both legacy "instance" parameter and new "toolset" parameter for backward compatibility.
 * 
 * Note: Tool objects are created dynamically, NOT as Spring beans, to prevent startup-time registration.
 */
@Service
public class InstanceToolManager {

    private final InstanceContext instanceContext;

    public InstanceToolManager(InstanceContext instanceContext) {
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
     * Get tool objects for a specific instance/toolset.
     * Based on the instance/toolset parameter, different sets of tools will be returned.
     * Supports both legacy "instance" parameter and new "toolset" parameter.
     * 
     * Tools are created dynamically (NOT as Spring beans) to prevent startup-time registration.
     *
     * @param instanceOrToolset Instance or toolset identifier from the connection URL or request
     * @return List of tool objects for this instance/toolset
     */
    public List<Object> getToolObjectsForInstance(String instanceOrToolset) {
        if (instanceOrToolset == null || instanceOrToolset.trim().isEmpty()) {
            instanceOrToolset = "default";
        }

        String key = instanceOrToolset.trim().toLowerCase();
        List<Object> toolObjects = new ArrayList<>();

        switch (key) {
            case "example1":
            case "instance1":
            case "example-tools":
                // Create ExampleTools dynamically (not as Spring bean)
                ExampleTools exampleTools = new ExampleTools();
                exampleTools.setInstanceContext(instanceContext);
                toolObjects.add(exampleTools);
                break;

            case "example2":
            case "instance2":
            case "example2-tools":
                // Create Example2Tools dynamically (not as Spring bean)
                Example2Tools example2Tools = new Example2Tools();
                example2Tools.setInstanceContext(instanceContext);
                toolObjects.add(example2Tools);
                break;

            case "all":
            case "both":
                // Create both tool sets dynamically
                ExampleTools et = new ExampleTools();
                et.setInstanceContext(instanceContext);
                Example2Tools e2t = new Example2Tools();
                e2t.setInstanceContext(instanceContext);
                toolObjects.add(et);
                toolObjects.add(e2t);
                break;

            default:
                // For unknown instances/toolsets, return default tools
                ExampleTools defaultTools = new ExampleTools();
                defaultTools.setInstanceContext(instanceContext);
                toolObjects.add(defaultTools);
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
