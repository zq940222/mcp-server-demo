# MCP 连接配置示例 - 工具集路由

本文档展示如何使用新的运行时动态工具集路由功能连接 MCP Server。

## 连接方式对比

### 方式1: 使用 HTTP Header (推荐)

使用 `X-Toolset` Header 指定工具集，这是最灵活的方式：

```json
{
  "mcpServers": {
    "maintainx-1": {
      "url": "https://app.crewplus.ai/mcp",
      "headers": {
        "x-api-key": "sk-3dnnhLMj9me9UkJtl8RltMHWJlOruexQGXaevXYkAnVAelwq",
        "X-Toolset": "maintainx-tools"
      },
      "transport": "streamable_http",
      "description": "MAINTAINX_CMMS (MaintainX CMMS Integration) - OpsMate AI Test CMMS"
    },
    "example-tools": {
      "url": "https://app.crewplus.ai/mcp",
      "headers": {
        "X-Toolset": "example-tools"
      },
      "transport": "streamable_http",
      "description": "Example tools: calculator, greeting, getCurrentTime"
    },
    "example2-tools": {
      "url": "https://app.crewplus.ai/mcp",
      "headers": {
        "X-Toolset": "example2-tools"
      },
      "transport": "streamable_http",
      "description": "Example2 tools: weather, temperature conversion, random number, string reverse"
    }
  }
}
```

### 方式2: 使用查询参数

在 URL 中使用 `toolset` 查询参数：

```json
{
  "mcpServers": {
    "maintainx-1": {
      "url": "https://app.crewplus.ai/mcp?toolset=maintainx-tools",
      "headers": {
        "x-api-key": "sk-3dnnhLMj9me9UkJtl8RltMHWJlOruexQGXaevXYkAnVAelwq"
      },
      "transport": "streamable_http",
      "description": "MAINTAINX_CMMS (MaintainX CMMS Integration)"
    }
  }
}
```

### 方式3: 向后兼容 - 使用 instance 参数

原有的 `instance` 参数仍然支持，会自动映射到对应的工具集：

```json
{
  "mcpServers": {
    "maintainx-1": {
      "url": "https://app.crewplus.ai/mcp?instance=37",
      "headers": {
        "x-api-key": "sk-3dnnhLMj9me9UkJtl8RltMHWJlOruexQGXaevXYkAnVAelwq"
      },
      "transport": "streamable_http",
      "description": "MAINTAINX_CMMS (MaintainX CMMS Integration)"
    }
  }
}
```

**注意**: `instance=37` 会被映射为 `toolset=37`，系统会尝试加载名为 `37` 的工具集，如果不存在则使用默认工具集。

### 方式4: 使用路径变量

使用 RESTful 风格的路径：

```json
{
  "mcpServers": {
    "maintainx-tools": {
      "url": "https://app.crewplus.ai/mcp/maintainx-tools",
      "headers": {
        "x-api-key": "sk-3dnnhLMj9me9UkJtl8RltMHWJlOruexQGXaevXYkAnVAelwq"
      },
      "transport": "streamable_http",
      "description": "MAINTAINX_CMMS Integration"
    }
  }
}
```

## 参数优先级

当同时使用多种方式时，优先级顺序为：

1. **HTTP Header** (`X-Toolset`) - 最高优先级
2. **查询参数** (`?toolset=xxx`)
3. **Legacy 实例参数** (`?instance=xxx`)
4. **路径变量** (`/mcp/{toolset}`)

例如，如果同时设置了 Header 和查询参数，Header 中的值会被使用。

## 完整配置示例

```json
{
  "mcpServers": {
    "langgraph-mcp": {
      "url": "https://crewplus-tools-dev-ead0f8abe78c573ba7340d7ee1355fde.us.langgraph.app/mcp",
      "headers": {
        "x-api-key": "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"
      },
      "transport": "streamable_http"
    },
    "amap-agent": {
      "url": "https://mcp.amap.com/sse?key=919bf4296dfed100ab29c07832640e6e",
      "tool_args": {
        "maps_direction_driving": {
          "origin": "121.4737,31.2304"
        },
        "maps_direction_transit_integrated": {
          "origin": "121.402942,31.131248"
        }
      },
      "transport": "sse"
    },
    "mysql": {
      "command": "npx",
      "args": [
        "-y",
        "@fhuang/mcp-mysql-server"
      ],
      "env": {
        "MYSQL_HOST": "127.0.0.1",
        "MYSQL_USER": "app",
        "MYSQL_PORT": "3306",
        "MYSQL_PASSWORD": "lcEfm2R5LZ",
        "MYSQL_DATABASE": "crewplus_app"
      }
    },
    "maintainx-1": {
      "url": "https://app.crewplus.ai/mcp",
      "headers": {
        "x-api-key": "sk-3dnnhLMj9me9UkJtl8RltMHWJlOruexQGXaevXYkAnVAelwq",
        "X-Toolset": "maintainx-tools"
      },
      "transport": "streamable_http",
      "description": "MAINTAINX_CMMS (MaintainX CMMS Integration) - OpsMate AI Test CMMS. This MCP server provides access to MaintainX CMMS system for managing work orders, assets, locations, parts inventory, and maintenance operations."
    },
    "example-tools": {
      "url": "https://app.crewplus.ai/mcp",
      "headers": {
        "X-Toolset": "example-tools"
      },
      "transport": "streamable_http",
      "description": "Example tools: calculator, greeting, getCurrentTime"
    },
    "example2-tools": {
      "url": "https://app.crewplus.ai/mcp",
      "headers": {
        "X-Toolset": "example2-tools"
      },
      "transport": "streamable_http",
      "description": "Example2 tools: weather, temperature conversion, random number, string reverse"
    },
    "all-tools": {
      "url": "https://app.crewplus.ai/mcp",
      "headers": {
        "X-Toolset": "all"
      },
      "transport": "streamable_http",
      "description": "All available tools from both example-tools and example2-tools"
    }
  }
}
```

## 迁移指南

### 从 instance 参数迁移到 toolset

**旧配置**:
```json
{
  "url": "https://app.crewplus.ai/mcp?instance=37"
}
```

**新配置（推荐）**:
```json
{
  "url": "https://app.crewplus.ai/mcp",
  "headers": {
    "X-Toolset": "maintainx-tools"
  }
}
```

或者保持向后兼容：
```json
{
  "url": "https://app.crewplus.ai/mcp?instance=37"
}
```

## 工具集名称映射

| instance 值 | 对应的 toolset | 说明 |
|------------|---------------|------|
| `example1` / `instance1` | `example-tools` | ExampleTools 工具集 |
| `example2` / `instance2` | `example2-tools` | Example2Tools 工具集 |
| `all` / `both` | `all` | 所有工具 |
| `37` | `37` | 自定义工具集（需要注册） |

## 注意事项

1. **工具集白名单**: 确保使用的工具集名称在 `application.properties` 的白名单中配置
2. **大小写不敏感**: toolset 名称会自动转换为小写
3. **缓存机制**: 工具集会被缓存以提高性能，默认缓存30分钟
4. **向后兼容**: 原有的 `instance` 参数仍然可用，无需立即迁移

## 验证连接

使用以下 API 端点验证工具集是否正确加载：

```bash
# 查询所有已注册的工具集
curl http://localhost:8083/api/tools/toolsets

# 查询特定工具集信息
curl http://localhost:8083/api/tools/toolset/maintainx-tools

# 健康检查
curl http://localhost:8083/api/tools/health
```

