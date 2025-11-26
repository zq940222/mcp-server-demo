# 自定义 MCP Server 框架文档

## 概述

由于 Spring AI MCP Server 不支持真正的运行时动态工具发现，我们实现了一个**自定义的 MCP Server 框架**，完全绕过 Spring AI MCP Server 的限制，支持真正的动态工具发现。

## 架构设计

### 核心组件

1. **CustomMcpServer** (`ai.crewplus.mcpserver.mcp.CustomMcpServer`)
   - 自定义 MCP Server 核心实现
   - 管理工具定义和缓存
   - 支持动态工具发现和执行
   - 集成 Multi-MCP Hub 模式

2. **CustomMcpController** (`ai.crewplus.mcpserver.mcp.CustomMcpController`)
   - REST Controller 实现 MCP 协议
   - 支持 JSON-RPC over HTTP
   - 处理 initialize、tools/list、tools/call 请求

3. **ToolDefinition** (CustomMcpServer 内部类)
   - 工具定义封装
   - 包含工具名称、描述、输入 Schema
   - 支持工具调用和参数转换

## 功能特性

### ✅ 真正的动态工具发现
- 工具在连接时动态加载
- 不在启动时注册所有工具
- 根据 toolset 参数动态提供工具

### ✅ Multi-MCP Hub 集成
- 集成现有的 `McpHub` 和 `McpInstance`
- 支持多实例管理
- 每个 toolset 独立工具集

### ✅ MCP 协议支持
- 支持标准 MCP 协议（JSON-RPC 2.0）
- 支持 `initialize`、`tools/list`、`tools/call` 方法
- 兼容 MCP 客户端

## API 端点

### 1. 初始化 (`/custom-mcp/initialize`)

```http
POST /custom-mcp/initialize
X-Toolset: example-tools
Content-Type: application/json

{
  "protocolVersion": "2025-06-18",
  "capabilities": {},
  "clientInfo": {
    "name": "client-name",
    "version": "1.0.0"
  }
}
```

**响应：**
```json
{
  "protocolVersion": "2025-06-18",
  "capabilities": {
    "tools": {
      "listChanged": false
    }
  },
  "serverInfo": {
    "name": "custom-mcp-server",
    "version": "1.0.0"
  }
}
```

### 2. 列出工具 (`/custom-mcp/tools/list`)

```http
POST /custom-mcp/tools/list
X-Toolset: example-tools
```

**响应：**
```json
{
  "tools": [
    {
      "name": "calculator",
      "description": "Perform basic arithmetic operations",
      "inputSchema": {
        "type": "object",
        "properties": {
          "operation": {
            "type": "string",
            "description": "The operation to perform"
          },
          "num1": {
            "type": "number",
            "description": "First number"
          },
          "num2": {
            "type": "number",
            "description": "Second number"
          }
        },
        "required": ["operation", "num1", "num2"]
      }
    }
  ]
}
```

### 3. 调用工具 (`/custom-mcp/tools/call`)

```http
POST /custom-mcp/tools/call
X-Toolset: example-tools
Content-Type: application/json

{
  "name": "calculator",
  "arguments": {
    "operation": "add",
    "num1": 10,
    "num2": 20
  }
}
```

**响应：**
```json
{
  "content": [
    {
      "type": "text",
      "text": "30.0"
    }
  ]
}
```

### 4. JSON-RPC 端点 (`/custom-mcp/`)

支持标准 JSON-RPC 2.0 协议：

```http
POST /custom-mcp/
X-Toolset: example-tools
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/list",
  "params": {}
}
```

## 使用方式

### 1. 配置客户端

修改 MCP 客户端配置，指向自定义端点：

```json
{
  "mcpServers": {
    "custom-server": {
      "url": "http://localhost:8083/custom-mcp/",
      "headers": {
        "X-Toolset": "example-tools"
      }
    }
  }
}
```

### 2. 工具集路由

通过 HTTP Header 或 Query 参数指定 toolset：

```bash
# 使用 Header
curl -H "X-Toolset: example-tools" \
     http://localhost:8083/custom-mcp/tools/list

# 使用 Query 参数
curl http://localhost:8083/custom-mcp/tools/list?toolset=example-tools
```

### 3. 动态工具发现

工具会根据 toolset 参数动态加载：

- `toolset=example-tools` → ExampleTools（3个工具）
- `toolset=example2-tools` → Example2Tools（4个工具）
- 未指定 toolset → 返回空工具列表

## 优势

1. **完全控制**：不依赖 Spring AI MCP Server 的限制
2. **真正的动态发现**：工具在连接时动态加载
3. **Multi-MCP Hub 集成**：复用现有的 Hub 架构
4. **标准协议**：支持 MCP 标准协议
5. **易于扩展**：可以轻松添加新功能

## 与 Spring AI MCP Server 的区别

| 特性 | Spring AI MCP Server | 自定义 MCP Server |
|------|---------------------|------------------|
| 动态工具发现 | ❌ 不支持 | ✅ 完全支持 |
| 运行时工具注册 | ❌ 不支持 | ✅ 完全支持 |
| Multi-Instance | ❌ 不支持 | ✅ 完全支持 |
| 框架依赖 | ✅ 依赖框架 | ❌ 独立实现 |
| 协议兼容性 | ✅ 标准协议 | ✅ 标准协议 |

## 未来改进

1. **WebSocket 支持**：支持 WebSocket 连接
2. **工具缓存优化**：更智能的工具缓存策略
3. **错误处理增强**：更详细的错误信息
4. **性能优化**：工具调用性能优化
5. **监控和日志**：添加详细的监控和日志

## 迁移指南

如果要从 Spring AI MCP Server 迁移到自定义 MCP Server：

1. 更新客户端配置，指向 `/custom-mcp/` 端点
2. 确保工具类使用 `@DynamicToolset` 注解
3. 通过 `X-Toolset` Header 或 Query 参数指定 toolset
4. 测试工具调用是否正常工作

