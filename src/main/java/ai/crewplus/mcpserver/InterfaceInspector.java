package ai.crewplus.mcpserver;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
public class InterfaceInspector implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        try {
            // Check SyncMcpToolProvider hierarchy
            Class<?> clazz = Class.forName("org.springaicommunity.mcp.provider.tool.SyncMcpToolProvider");
            System.out.println("Hierarchy of SyncMcpToolProvider:");
            printHierarchy(clazz);

            // Try to find McpToolProvider interface
            String[] packages = {
                "org.springaicommunity.mcp.spec.server",
                "org.springframework.ai.mcp.spec.server",
                "org.springaicommunity.mcp.server",
                "org.springframework.ai.mcp.server"
            };

            for (String pkg : packages) {
                try {
                    String interfaceName = pkg + ".McpToolProvider";
                    Class<?> interfaceClass = Class.forName(interfaceName);
                    System.out.println("Found interface: " + interfaceName);
                    System.out.println("Is Interface? " + interfaceClass.isInterface());
                } catch (ClassNotFoundException e) {
                    // ignore
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printHierarchy(Class<?> clazz) {
        if (clazz == null) return;
        System.out.println("Class: " + clazz.getName());
        System.out.println("Interfaces:");
        Arrays.stream(clazz.getInterfaces()).forEach(i -> System.out.println(" - " + i.getName()));
        printHierarchy(clazz.getSuperclass());
    }
}
