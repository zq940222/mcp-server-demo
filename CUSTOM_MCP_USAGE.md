# 自定义 MCP Server 使用指南

## 概述

由于 Spring AI MCP Server 不支持真正的运行时动态工具发现，我们实现了一个**完全自定义的 MCP Server 框架**，完全绕过 Spring AI MCP Server 的限制。

## 快速测试

### 1. 测试端点

访问测试端点验证自定义 MCP Server 是否工作：

```bash
# 使用 HTTP Header
curl -H "X-Toolset: example-tools" http://localhost:8083/custom-mcp/test

# 使用 Query 参数
curl http://localhost:8083/custom-mcp/test?toolset=example-tools
```

**预期响应：**
```json
{
  "status": "ok",
  "toolset": "example-tools",
  "toolCount": 3,
  "tools": [
    {
      "name": "calculator",
      "description": "...",
      "inputSchema": {...}
    },
    ...
  ],
  "message": "Custom MCP Server is working!"
}
```

### 2. 列出工具

```bash
curl -X POST -H "X-Toolset: example-tools" \
     -H "Content-Type: application/json" \
     http://localhost:8083/custom-mcp/tools/list
```

### 3. 调用工具

```bash
curl -X POST -H "X-Toolset: example-tools" \
     -H "Content-Type: application/json" \
     -d '{
       "name": "calculator",
       "arguments": {
         "operation": "add",
         "num1": 10,
         "num2": 20
       }
     }' \
     http://localhost:8083/custom-mcp/tools/call
```

## 客户端配置

### 方案1：使用自定义端点（推荐）

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

### 方案2：使用标准端点（如果 Spring AI MCP Server 被禁用）

如果成功覆盖了 `/mcp/*` 端点：

```json
{
  "mcpServers": {
    "custom-server": {
      "url": "http://localhost:8083/mcp/",
      "headers": {
        "X-Toolset": "example-tools"
      }
    }
  }
}
```

## API 端点

### 1. 测试端点
- `GET /custom-mcp/test?toolset=example-tools` - 测试自定义 MCP Server

### 2. 初始化
- `POST /custom-mcp/initialize` - 初始化 MCP 连接
- `POST /mcp/initialize` - 同上（如果覆盖成功）

### 3. 列出工具
- `POST /custom-mcp/tools/list` - 列出可用工具
- `POST /mcp/tools/list` - 同上

### 4. 调用工具
- `POST /custom-mcp/tools/call` - 调用工具
- `POST /mcp/tools/call` - 同上

### 5. JSON-RPC 端点
- `POST /custom-mcp/` - JSON-RPC 2.0 端点
- `POST /mcp/` - 同上

## 工具集路由

通过以下方式指定 toolset：

1. **HTTP Header**（推荐）：
   ```
   X-Toolset: example-tools
   ```

2. **Query 参数**：
   ```
   ?toolset=example-tools
   ```

3. **请求体**（仅 initialize）：
   ```json
   {
     "toolset": "example-tools"
   }
   ```

## 故障排查

### 问题：测试端点返回空工具列表

1. 检查 toolset 参数是否正确传递
2. 查看日志中的 `CustomMcpServer.initialize()` 输出
3. 确认 `McpHub.getInstance()` 是否成功加载实例

### 问题：工具调用失败

1. 检查工具名称是否正确
2. 检查参数名称和类型是否匹配
3. 查看日志中的错误信息

### 问题：客户端看不到工具

1. 确认客户端配置指向 `/custom-mcp/` 端点
2. 确认 `X-Toolset` Header 已设置
3. 测试 `/custom-mcp/test` 端点验证工具是否加载成功

## 下一步

1. **测试自定义端点**：访问 `/custom-mcp/test?toolset=example-tools` 验证功能
2. **配置客户端**：修改 MCP 客户端配置使用自定义端点
3. **验证工具调用**：测试工具调用是否正常工作

如果测试端点返回了工具列表，说明自定义 MCP Server 工作正常！

