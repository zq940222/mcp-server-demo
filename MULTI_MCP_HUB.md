# Multi-MCP Hub 模式架构文档

## 概述

Multi-MCP Hub 模式是一个中心化的 MCP 实例管理器，支持多个独立的工具集（toolsets）动态加载和管理。每个工具集对应一个 MCP 实例，可以根据请求参数动态路由到对应的实例。

## 架构设计

### 核心组件

1. **McpHub** (`ai.crewplus.mcpserver.hub.McpHub`)
   - 中心化的 Hub 管理器
   - 管理所有 MCP 实例
   - 提供实例的获取、注册、移除等功能
   - 支持实例缓存和动态加载

2. **McpInstance** (`ai.crewplus.mcpserver.hub.McpInstance`)
   - 代表一个独立的 MCP 实例
   - 包含工具列表、元数据（名称、描述等）
   - 记录创建时间和最后访问时间

3. **InstanceLoader** (`ai.crewplus.mcpserver.hub.InstanceLoader`)
   - 动态加载 MCP 实例
   - 扫描 `@DynamicToolset` 注解
   - 从 `DynamicToolsetRegistry` 加载工具
   - 缓存实例元数据

4. **HubToolProvider** (`ai.crewplus.mcpserver.hub.HubToolProvider`)
   - 动态工具提供者
   - 根据当前请求的 toolset 参数返回对应的工具
   - 从 `McpHub` 获取实例，然后返回实例的工具列表

5. **HubConfig** (`ai.crewplus.mcpserver.hub.HubConfig`)
   - Hub 模式的配置类
   - 创建自定义的 `SyncMcpToolProvider` Bean
   - 使用 `HubToolProvider` 提供动态工具

## 工作流程

### 1. 启动阶段

1. `InstanceLoader` 扫描所有 `@DynamicToolset` 注解的类
2. 为每个工具集创建 `InstanceMetadata` 并缓存
3. `McpHub` 初始化（此时实例尚未加载）

### 2. 请求阶段

1. `DynamicToolsetInterceptor` 拦截请求，提取 `toolset` 参数
2. 设置 `toolset` 到 `InstanceContext`（ThreadLocal）
3. `HubToolProvider.getToolObjects()` 被调用：
   - 从 `InstanceContext` 获取当前 `toolset`
   - 调用 `McpHub.getInstance(toolset)` 获取实例
   - `McpHub` 检查实例是否存在，不存在则调用 `InstanceLoader.loadInstance()`
   - `InstanceLoader` 从 `DynamicToolsetRegistry` 加载工具
   - 创建 `McpInstance` 并缓存
   - 返回工具列表
4. Spring AI MCP Server 使用返回的工具列表处理请求

## 使用方式

### 1. 定义工具集

使用 `@DynamicToolset` 注解标记工具类：

```java
@DynamicToolset(
    value = {"example-tools", "example1", "instance1"},
    name = "Example Tools",
    description = "Basic example tools: calculator, greeting, current time"
)
public class ExampleTools {
    // ... tool methods
}
```

### 2. 连接 MCP Server

通过 HTTP Header、Query 参数或 Path 变量指定 toolset：

```bash
# 使用 HTTP Header
curl -H "X-Toolset: example-tools" http://localhost:8083/mcp

# 使用 Query 参数
curl http://localhost:8083/mcp?toolset=example-tools

# 使用 Path 变量（如果支持）
curl http://localhost:8083/mcp/example-tools
```

### 3. 工具集路由

- `toolset=example-tools` → ExampleTools（3个工具）
- `toolset=example2-tools` → Example2Tools（4个工具）
- 未指定 toolset → 返回空工具列表

## 优势

1. **真正的动态发现**：工具在连接时动态加载，不在启动时注册
2. **多实例支持**：每个工具集独立管理，互不干扰
3. **实例缓存**：已加载的实例会被缓存，提高性能
4. **灵活的路由**：支持多种方式指定 toolset（Header、Query、Path）
5. **易于扩展**：添加新工具集只需添加 `@DynamicToolset` 注解

## 与之前方案的区别

### 之前的方案（动态工具注册）
- 尝试在运行时注册工具到 Spring AI MCP Server 的内部注册表
- 使用反射直接操作 `ServerMcpAnnotatedBeans`
- 问题：Spring AI MCP Server 在启动时已经扫描并缓存了工具列表

### Multi-MCP Hub 模式
- 不依赖 Spring AI MCP Server 的内部注册表
- 通过 `HubToolProvider` 在每次请求时动态提供工具
- 使用 `@Primary` 注解替换默认的 `SyncMcpToolProvider`
- 优势：完全控制工具的提供逻辑，不受框架限制

## 配置说明

### application.properties

```properties
# MCP Server 配置
server.port=8083

# Toolset 安全配置（可选）
mcp.toolset.allowed=example-tools,example2-tools
mcp.toolset.cache.maxSize=10
mcp.toolset.cache.expireAfterWrite=PT30M
```

## 故障排查

### 问题：工具不显示

1. 检查 `toolset` 参数是否正确传递
2. 检查 `@DynamicToolset` 注解是否正确配置
3. 查看日志中的 `HubToolProvider` 输出
4. 确认 `HubConfig` 中的 Bean 是否创建成功

### 问题：实例未加载

1. 检查 `InstanceLoader` 是否扫描到工具集
2. 查看 `DynamicToolsetRegistry` 的日志
3. 确认工具类是否正确实现

## 未来改进

1. **实例生命周期管理**：支持实例的卸载和重新加载
2. **实例健康检查**：监控实例的状态
3. **实例统计**：记录每个实例的使用情况
4. **动态配置**：支持运行时添加/移除工具集
5. **实例隔离**：更严格的实例间隔离机制

