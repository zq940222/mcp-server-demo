# 启动时不注册工具，连接时动态发现

## 实现方案

已实现启动时不注册任何工具，仅在连接时动态发现和注册工具的功能。

## 关键变更

### 1. 移除启动时的Bean定义

**McpServerConfig.java** - 移除了所有工具Bean的定义：
```java
@Configuration
public class McpServerConfig {
    // No bean definitions here - tools are created dynamically at connection time
    // This prevents Spring AI MCP Server from auto-scanning and registering tools at startup
}
```

### 2. 动态创建工具对象

**InstanceToolManager.java** - 工具对象现在在运行时动态创建，而不是作为Spring Bean：
```java
public List<Object> getToolObjectsForInstance(String instanceOrToolset) {
    // Create ExampleTools dynamically (not as Spring bean)
    ExampleTools exampleTools = new ExampleTools();
    exampleTools.setInstanceContext(instanceContext);
    toolObjects.add(exampleTools);
    // ...
}
```

### 3. 配置禁用自动扫描

**application.properties** - 添加配置禁用自动工具扫描：
```properties
# Disable automatic tool scanning at startup
spring.ai.mcp.server.annotation-scanner.enabled=false
```

## 工作流程

### 启动时
1. Spring AI MCP Server启动
2. **不扫描任何工具Bean**（因为没有定义）
3. 日志显示：`Registered tools: 0`（或更少）

### 连接时
1. 客户端发送请求，携带toolset参数（Header: `X-Toolset` 或 Query: `?toolset=xxx`）
2. `InstanceToolInterceptor` 提取toolset参数并设置到ThreadLocal
3. `ToolsetRouter` 根据toolset动态加载工具对象
4. `DynamicToolsetLoader` 创建工具实例（不是Spring Bean）
5. 工具在连接时被注册和使用

## 验证方法

### 1. 检查启动日志

启动时应该看到：
```
Registered tools: 0
```

而不是：
```
Registered tools: 7
```

### 2. 连接时日志

连接时应该看到：
```
Dynamic tool discovery for toolset: example-tools
Dynamically loaded toolset: example-tools
Providing 1 tools for toolset: example-tools
```

### 3. API测试

```bash
# 使用toolset参数连接
curl "http://localhost:8083/mcp?toolset=example-tools"

# 或使用Header
curl -H "X-Toolset: example-tools" http://localhost:8083/mcp
```

## 优势

1. **零启动注册**: 启动时不注册任何工具，减少启动时间和内存占用
2. **动态发现**: 工具在连接时根据toolset参数动态发现和加载
3. **按需加载**: 只加载当前连接需要的工具集
4. **灵活扩展**: 可以动态添加新工具集而无需重启服务

## 注意事项

1. **配置检查**: 确保 `spring.ai.mcp.server.annotation-scanner.enabled=false` 已设置
2. **工具集白名单**: 确保使用的工具集在白名单中配置
3. **首次连接**: 首次连接时会有轻微的加载延迟（工具集被缓存后延迟消失）

## 对比

| 特性 | 启动时注册 | 连接时动态发现 |
|-----|----------|--------------|
| 启动时间 | 较慢（扫描所有工具） | 快（不扫描） |
| 内存占用 | 高（所有工具常驻） | 低（按需加载） |
| 灵活性 | 低（需重启添加工具） | 高（动态添加） |
| 首次连接延迟 | 无 | 轻微（加载工具集） |

## 故障排查

### 问题：启动时仍然注册了工具

**原因**: 可能有其他地方定义了工具Bean

**解决**: 
1. 检查是否有其他`@Bean`方法创建工具对象
2. 检查工具类是否被`@Component`或`@Service`注解
3. 确认`spring.ai.mcp.server.annotation-scanner.enabled=false`配置生效

### 问题：连接时工具未加载

**原因**: toolset参数未正确传递或工具集未注册

**解决**:
1. 检查请求是否包含toolset参数（Header或Query）
2. 检查工具集是否在白名单中
3. 查看日志确认工具集加载过程

