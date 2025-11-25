# Spring AI Alibaba 动态工具注册实现说明

## 概述

本项目使用 Spring AI Alibaba 的 ToolRegistry 实现根据 `instance` 参数动态注册不同的工具集。

## 架构设计

### 1. 工具类 (`ExampleTools`, `Example2Tools`)
- 工具类不再使用 `@Service` 或 `@Component` 注解
- 工具实现为静态内部类，实现 `Function<Request, String>` 接口
- 每个工具都是独立的函数，可以动态注册到 ToolRegistry

### 2. 工具管理器 (`InstanceToolManager`)
- 根据 `instance` 参数注册不同的工具集
- 使用反射调用 Spring AI Alibaba 的 ToolRegistry API
- 支持的工具集：
  - `instance1` / `example1`: ExampleTools (3个工具)
  - `instance2` / `example2`: Example2Tools (4个工具)
  - `all` / `both`: 所有工具 (7个工具)
  - 默认: ExampleTools (3个工具)

### 3. MCP Server 生命周期处理器 (`McpServerLifecycleHandler`)
- 监听 MCP Session 创建事件
- 从 Session 初始化参数中提取 `instance` 参数
- 获取 Session 的 ToolRegistry 并注册相应的工具

## 使用方法

### 连接 MCP Server

```
http://localhost:8083/mcp?instance=instance1
```

这将注册 ExampleTools 的工具集（calculator, greeting, getCurrentTime）

```
http://localhost:8083/mcp?instance=instance2
```

这将注册 Example2Tools 的工具集（getWeather, convertTemperature, generateRandomNumber, reverseString）

### API 端点

- `GET /api/tools/instance/{instance}` - 查询指定 instance 的工具列表
- `GET /api/tools/health` - 健康检查

## 注意事项

1. **ToolRegistry API**: 代码使用反射调用 ToolRegistry，因为 Spring AI Alibaba 的具体 API 可能因版本而异。如果遇到问题，请检查实际的 ToolRegistry 接口。

2. **Session 生命周期**: `McpServerLifecycleHandler` 需要在 Spring AI Alibaba MCP Server 识别并调用。如果框架不支持自动发现，可能需要实现特定的接口或使用特定的注解。

3. **依赖配置**: 确保 `spring-ai-alibaba-starter-mcp-gateway` 依赖已正确添加。

## 后续优化建议

1. 如果 Spring AI Alibaba 提供了明确的接口，可以替换反射调用
2. 可以添加工具注册的日志记录
3. 可以添加工具注册失败的重试机制
4. 可以支持从配置文件或数据库加载工具配置

