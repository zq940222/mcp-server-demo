# 运行时动态工具集路由方案

## 概述

本方案实现了基于连接时参数的动态工具集路由，允许在**不重启服务**的情况下，根据每次请求的参数（如HTTP Header、查询参数等）动态加载和执行对应的工具集。这就像一个智能分拣员，实时查看每个请求的"快递单"，然后把它分发到正确的处理流水线上。

## 架构设计

### 核心组件

1. **ToolsetRouter** - 工具集路由管理器
   - 负责根据toolset参数路由到不同的工具集
   - 使用缓存优化性能
   - 线程安全设计

2. **DynamicToolsetLoader** - 动态工具集加载器
   - 支持从类路径或外部JAR文件加载工具集
   - 实现SPI机制进行动态加载
   - 白名单安全控制

3. **ToolsetPool** - 工具集缓存池
   - 缓存频繁使用的工具集实例
   - 支持过期时间和大小限制
   - 线程安全的缓存实现

4. **InstanceToolInterceptor** - 请求拦截器
   - 从HTTP Header、查询参数等提取toolset参数
   - 设置ThreadLocal上下文
   - 支持多种参数传递方式

## 参数传递方式

| 参数传递方式 | 实现示例 | 适用场景 |
|------------|---------|---------|
| HTTP Header | `X-Toolset: order-tools` | RESTful API 调用 |
| 查询参数 | `/mcp?toolset=weather-tools` | 简单场景 |
| 请求体字段 | `{"toolset": "payment-tools"}` | JSON-RPC 协议 |
| 路径变量 | `/mcp/order-tools` | RESTful 路径风格 |
| Legacy参数 | `/mcp?instance=example1` | 向后兼容 |

### 优先级顺序

1. HTTP Header (`X-Toolset`)
2. 查询参数 (`?toolset=xxx`)
3. Legacy实例参数 (`?instance=xxx`)
4. 路径变量 (`/mcp/{toolset}`)

## 使用方法

### 1. 通过HTTP Header指定工具集

```bash
curl -X POST http://localhost:8083/mcp \
  -H "X-Toolset: example-tools" \
  -H "Content-Type: application/json" \
  -d '{"method": "tools/list"}'
```

### 2. 通过查询参数指定工具集

```bash
curl "http://localhost:8083/mcp?toolset=example2-tools"
```

### 3. 通过路径变量指定工具集

```bash
curl "http://localhost:8083/mcp/example-tools"
```

### 4. 向后兼容的instance参数

```bash
curl "http://localhost:8083/mcp?instance=example1"
```

## 配置说明

### application.properties

```properties
# 工具集白名单（逗号分隔）
# 空列表表示允许所有工具集（生产环境不推荐）
mcp.toolset.allowed=example-tools,example2-tools,order-tools,weather-tools,payment-tools

# 工具集缓存配置
mcp.toolset.cache.max-size=10
mcp.toolset.cache.expire-minutes=30
```

### 安全配置

- **白名单机制**: 只有配置在白名单中的工具集才能被加载
- **类加载限制**: 防止任意类加载，降低安全风险
- **日志监控**: 记录工具集加载失败事件

## 预定义工具集

| 工具集名称 | 别名 | 包含工具 |
|----------|------|---------|
| `example-tools` | `example1`, `instance1` | calculator, greeting, getCurrentTime |
| `example2-tools` | `example2`, `instance2` | getWeather, convertTemperature, generateRandomNumber, reverseString |
| `all` | `both` | 所有工具 |

## API端点

### 查询工具集信息

```bash
# 获取所有已注册的工具集
GET /api/tools/toolsets

# 获取特定工具集的信息
GET /api/tools/toolset/{toolset}

# 获取特定实例的工具（向后兼容）
GET /api/tools/instance/{instance}

# 健康检查
GET /api/tools/health
```

## 动态加载新工具集

### 方式1: 通过类名加载

系统会自动尝试从以下包路径加载工具集类：
- `ai.crewplus.mcpserver.tool.{ToolsetName}`
- `com.example.{ToolsetName}`

例如，工具集名称 `order-tools` 会尝试加载：
- `ai.crewplus.mcpserver.tool.OrderTools`
- `com.example.OrderTools`

### 方式2: 通过JAR文件加载

```java
DynamicToolsetLoader loader = ...;
Object toolsetInstance = loader.loadToolsetFromJar(
    "/path/to/order-tools.jar",
    "com.example.OrderTools"
);
```

### 方式3: 编程式注册

```java
@Autowired
private ToolsetRouter toolsetRouter;

public void registerCustomToolset() {
    List<Object> toolObjects = Arrays.asList(
        new OrderTools(),
        new PaymentTools()
    );
    toolsetRouter.registerToolset("custom-tools", toolObjects);
}
```

## 完整调用流程

```
客户端请求
  ↓
InstanceToolInterceptor (提取toolset参数)
  ↓
ToolsetRouter (路由到对应工具集)
  ↓
ToolsetPool (检查缓存)
  ↓
DynamicToolsetLoader (动态加载)
  ↓
工具执行
  ↓
返回结果
```

## 性能优化

1. **缓存机制**: 使用ToolsetPool缓存频繁使用的工具集
2. **预加载**: 在拦截器中预加载工具集到缓存
3. **线程安全**: 使用ConcurrentHashMap确保并发安全
4. **过期策略**: 自动清理过期缓存条目

## 注意事项

1. **安全控制**: 生产环境必须配置工具集白名单
2. **版本兼容**: 确保不同工具集的依赖版本兼容
3. **内存管理**: 注意缓存大小限制，避免内存溢出
4. **监控告警**: 建议添加工具集加载失败的监控埋点

## 示例代码

### 创建自定义工具集

```java
package ai.crewplus.mcpserver.tool;

import ai.crewplus.mcpserver.service.InstanceContext;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;

public class OrderTools {
    
    private InstanceContext instanceContext;
    
    public void setInstanceContext(InstanceContext instanceContext) {
        this.instanceContext = instanceContext;
    }
    
    @McpTool(description = "Query order information")
    public String queryOrder(
            @McpToolParam(description = "Order ID", required = true) String orderId) {
        return "Order " + orderId + " details...";
    }
}
```

### 使用工具集

```bash
# 使用order-tools工具集
curl -X POST http://localhost:8083/mcp \
  -H "X-Toolset: order-tools" \
  -H "Content-Type: application/json" \
  -d '{
    "method": "tools/call",
    "params": {
      "name": "queryOrder",
      "arguments": {
        "orderId": "12345"
      }
    }
  }'
```

## 与启动时区分的对比

| 指标 | 启动时区分 | 连接时区分 |
|-----|----------|----------|
| 内存占用 | 低（单工具集） | 中（多工具集缓存） |
| 首次响应时间 | 快 | 稍慢（需加载） |
| 运行时灵活性 | ❌ 不可变 | ✅ 动态切换 |
| 扩展性 | 需重启服务 | 热加载新工具集 |

## 总结

通过连接时参数路由方案，你可以在**不重启服务**的情况下动态切换工具集，特别适合：
- 多租户场景
- A/B测试
- 灰度发布
- 动态功能开关

这个方案将"决策"从启动时转移到运行时，提供了更高的灵活性和可扩展性。

