# 动态工具集发现框架

## 概述

这是一个支持**真正的动态工具发现**的框架实现。工具类使用 `@DynamicToolset` 注解标记后，将：

- ✅ **启动时不注册**：工具不会被Spring AI MCP Server在启动时扫描和注册
- ✅ **连接时动态发现**：工具在连接时根据toolset参数动态发现和注册
- ✅ **按需加载**：只加载匹配toolset的工具，节省内存

## 核心组件

### 1. @DynamicToolset 注解

用于标记需要动态发现的工具类：

```java
@DynamicToolset(value = {"example-tools", "example1"}, 
                name = "Example Tools",
                description = "Basic example tools")
public class ExampleTools {
    @McpTool(description = "...")
    public String calculator(...) { ... }
}
```

**注解参数**：
- `value`: 工具集标识符数组（必填）
- `name`: 工具集显示名称（可选）
- `description`: 工具集描述（可选）
- `enabledByDefault`: 是否默认启用（可选，默认false）

### 2. DynamicToolsetRegistry

管理动态工具集的注册和发现：

- 扫描所有 `@DynamicToolset` 注解的工具类
- 在连接时根据toolset参数动态创建和注册工具实例
- 缓存工具实例以提高性能

### 3. DynamicToolsetInterceptor

WebFilter拦截器，在连接时触发动态发现：

- 从请求中提取toolset参数（Header、Query、Path等）
- 调用 `DynamicToolsetRegistry` 进行动态发现
- 设置ThreadLocal上下文

### 4. DynamicToolsetBeanPostProcessor

BeanPostProcessor，确保 `@DynamicToolset` 工具类不在启动时被注册。

## 使用方法

### 步骤1: 标记工具类

在工具类上添加 `@DynamicToolset` 注解：

```java
package ai.crewplus.mcpserver.tool;

import ai.crewplus.mcpserver.annotation.DynamicToolset;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;

@DynamicToolset(value = {"order-tools", "order"},
                name = "Order Management Tools",
                description = "Tools for managing orders")
public class OrderTools {
    
    @McpTool(description = "Query order information")
    public String queryOrder(
            @McpToolParam(description = "Order ID", required = true) String orderId) {
        return "Order " + orderId + " details...";
    }
    
    @McpTool(description = "Create a new order")
    public String createOrder(...) {
        // ...
    }
}
```

**重要**：
- ❌ **不要**使用 `@Service` 或 `@Component` 注解
- ✅ **只使用** `@DynamicToolset` 注解
- ✅ 工具类必须是普通的POJO类

### 步骤2: 配置连接

客户端连接时指定toolset参数：

**方式1: HTTP Header**
```json
{
  "mcpServers": {
    "order-tools": {
      "url": "http://localhost:8083/mcp",
      "headers": {
        "X-Toolset": "order-tools"
      },
      "transport": "streamable_http"
    }
  }
}
```

**方式2: 查询参数**
```json
{
  "mcpServers": {
    "order-tools": {
      "url": "http://localhost:8083/mcp?toolset=order-tools",
      "transport": "streamable_http"
    }
  }
}
```

### 步骤3: 验证

启动应用后，检查日志：

**启动时**：
```
✅ Scanned dynamic toolsets. Found 2 toolsets: [example-tools, example2-tools]
Registered tools: 0  // 没有工具被注册！
```

**连接时**：
```
🔍 Dynamic toolset discovery triggered for: example-tools
✅ Dynamically discovered 1 tools for toolset: example-tools
🔧 Dynamically discovered 3 tools for toolset: example-tools
```

## 工作流程

```
1. 应用启动
   ↓
2. DynamicToolsetRegistry 扫描 @DynamicToolset 注解的工具类
   ↓
3. 工具类被记录，但NOT注册为Spring Bean
   ↓
4. Spring AI MCP Server启动，注册工具数: 0
   ↓
5. 客户端连接，携带 toolset 参数
   ↓
6. DynamicToolsetInterceptor 提取 toolset 参数
   ↓
7. DynamicToolsetRegistry.getToolsForToolset() 被调用
   ↓
8. 动态创建工具实例并注册为Spring Bean
   ↓
9. Spring AI MCP Server扫描新注册的Bean，发现@McpTool方法
   ↓
10. 工具被注册并可用
```

## 优势

### 1. 真正的动态发现
- ✅ 启动时不注册任何工具
- ✅ 连接时按需发现和注册
- ✅ 只加载匹配的工具集

### 2. 内存优化
- ✅ 不使用的工具集不占用内存
- ✅ 按需加载，节省资源

### 3. 灵活扩展
- ✅ 添加新工具集无需重启服务
- ✅ 支持热加载（如果实现）

### 4. 多租户支持
- ✅ 不同连接使用不同工具集
- ✅ 完全隔离

## 示例

### 示例1: 基本工具集

```java
@DynamicToolset("calculator-tools")
public class CalculatorTools {
    @McpTool(description = "Add two numbers")
    public double add(double a, double b) {
        return a + b;
    }
}
```

### 示例2: 多工具集标识符

```java
@DynamicToolset(value = {"weather-tools", "weather", "meteo"},
                name = "Weather Tools",
                description = "Weather-related tools")
public class WeatherTools {
    @McpTool(description = "Get weather forecast")
    public String getForecast(String city) {
        // ...
    }
}
```

### 示例3: 条件启用

```java
@DynamicToolset(value = {"premium-tools"},
                enabledByDefault = false)  // 需要明确请求
public class PremiumTools {
    // Premium features...
}
```

## 注意事项

1. **不要使用@Service/@Component**：工具类只使用 `@DynamicToolset` 注解
2. **工具类必须是POJO**：不能是Spring管理的Bean
3. **InstanceContext注入**：如果工具类需要 `InstanceContext`，框架会自动注入
4. **工具类位置**：建议放在 `ai.crewplus.mcpserver.tool` 包下，或配置扫描路径

## 故障排查

### 问题1: 工具未被发现

**原因**：工具类可能不在扫描路径中

**解决**：
- 确保工具类在 `ai.crewplus.mcpserver.tool` 包下
- 或修改 `DynamicToolsetRegistry.scanPackageForDynamicToolsets()` 添加更多包

### 问题2: 启动时仍注册了工具

**原因**：工具类可能被其他方式注册为Bean

**解决**：
- 检查是否有 `@Service` 或 `@Component` 注解
- 检查 `McpServerConfig` 是否创建了Bean
- 确保只使用 `@DynamicToolset` 注解

### 问题3: 连接时工具未加载

**原因**：toolset参数未正确传递

**解决**：
- 检查请求Header或Query参数
- 查看日志确认toolset参数被提取
- 验证toolset名称是否匹配 `@DynamicToolset` 的value

## API参考

### DynamicToolsetRegistry

```java
// 获取工具集的所有工具
List<Object> tools = registry.getToolsForToolset("example-tools");

// 检查工具集是否注册
boolean registered = registry.isToolsetRegistered("example-tools");

// 获取所有已注册的工具集
Set<String> toolsets = registry.getRegisteredToolsets();

// 清除工具集缓存
registry.clearToolsetCache("example-tools");
```

## 总结

这个框架实现了**真正的动态工具发现**：

- ✅ 启动时不注册工具
- ✅ 连接时动态发现
- ✅ 按需加载工具集
- ✅ 完全支持多租户

通过 `@DynamicToolset` 注解，你可以轻松地将任何工具类标记为动态发现的工具集，实现真正的按需加载。

