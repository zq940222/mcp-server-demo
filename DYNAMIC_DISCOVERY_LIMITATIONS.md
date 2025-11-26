# 动态发现工具的限制说明

## 当前实现的限制

### 问题
**Spring AI MCP Server 1.1.0 的工作机制**：
- 在**启动时**扫描所有带有 `@McpTool` 注解的 Spring Bean
- 注册这些工具到 MCP Server
- **不支持**在运行时动态添加或移除工具

### 当前实现方式
1. **启动时**：所有工具Bean被创建和扫描，所有工具被注册
2. **运行时**：工具方法内部检查 `toolset` 参数，决定是否执行

这**不是真正的动态发现**，而是：
- ✅ 启动时注册所有工具
- ✅ 运行时根据toolset过滤可用性
- ❌ 不能真正按需加载工具
- ❌ 启动时仍会占用内存

## 为什么无法实现真正的动态发现？

### Spring AI MCP Server 的限制
1. **注解扫描机制**：框架在启动时扫描 `@McpTool` 注解，无法延迟到连接时
2. **Bean注册时机**：工具必须作为Spring Bean存在，才能被扫描
3. **缺少运行时API**：当前版本可能不提供 `addTool()` / `removeTool()` 等运行时API

### 可能的解决方案

#### 方案1：使用 McpSyncServer API（如果可用）
如果Spring AI MCP Server提供了 `McpSyncServer` 接口，可以尝试：

```java
@Autowired
private McpSyncServer mcpSyncServer;

public void registerToolsForToolset(String toolset) {
    List<Object> toolObjects = toolsetRouter.getToolObjectsForToolset(toolset);
    
    // 转换为工具规范并注册
    for (Object toolObject : toolObjects) {
        List<SyncToolSpecification> tools = McpToolUtils
            .toSyncToolSpecifications(ToolCallbacks.from(toolObject));
        
        for (SyncToolSpecification tool : tools) {
            mcpSyncServer.addTool(tool);
        }
    }
}
```

**问题**：需要确认 `spring-ai-starter-mcp-server-webflux` 是否提供此API。

#### 方案2：使用 BeanPostProcessor 过滤工具
在启动时阻止工具Bean被注册：

```java
@Component
public class ToolBeanFilter implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (hasMcpToolMethods(bean)) {
            // 返回null或代理对象，阻止注册
            return null;
        }
        return bean;
    }
}
```

**问题**：可能导致Spring AI MCP Server无法正常工作。

#### 方案3：使用条件Bean创建
只在特定条件下创建工具Bean：

```java
@Bean
@ConditionalOnProperty(name = "mcp.toolset.enabled", havingValue = "true")
public ExampleTools exampleTools() {
    // ...
}
```

**问题**：仍然需要在启动时决定，无法在连接时动态决定。

#### 方案4：自定义 ToolCallbackProvider（推荐尝试）
实现自定义的 `ToolCallbackProvider`，在每次请求时动态提供工具：

```java
@Bean
public ToolCallbackProvider toolCallbackProvider() {
    return () -> {
        String toolset = instanceContext.getCurrentInstance();
        List<Object> toolObjects = toolsetRouter.getToolObjectsForToolset(toolset);
        return MethodToolCallbackProvider.builder()
            .toolObjects(toolObjects.toArray())
            .build();
    };
}
```

**问题**：需要确认Spring AI MCP Server是否支持自定义 `ToolCallbackProvider`。

## 当前实现的优势

虽然无法实现真正的动态发现，但当前实现仍有价值：

1. **运行时过滤**：根据toolset参数动态决定工具是否可用
2. **多租户支持**：不同连接可以使用不同的工具集
3. **向后兼容**：支持legacy `instance` 参数
4. **易于实现**：不需要复杂的API调用

## 建议

### 短期方案（当前实现）
- ✅ 保持当前的运行时过滤机制
- ✅ 在工具方法中检查toolset参数
- ✅ 不匹配时返回错误信息

### 长期方案（如果框架支持）
1. **升级Spring AI版本**：检查新版本是否支持运行时工具注册
2. **使用McpSyncServer API**：如果可用，实现真正的动态注册
3. **自定义实现**：如果框架不支持，考虑自定义MCP Server实现

## 验证当前实现

### 启动日志
```
Registered tools: 7  // 所有工具都被注册
```

### 连接时行为
- 工具列表：显示所有7个工具
- 工具执行：根据toolset参数决定是否可用
- 不匹配的toolset：工具返回错误信息

## 总结

**当前限制**：
- ❌ 无法实现启动时不注册工具
- ❌ 无法实现连接时动态发现
- ✅ 可以实现运行时动态过滤

**如果必须实现真正的动态发现**，需要：
1. 确认Spring AI MCP Server是否提供运行时API
2. 或者考虑使用其他MCP Server实现
3. 或者等待框架更新支持此功能

