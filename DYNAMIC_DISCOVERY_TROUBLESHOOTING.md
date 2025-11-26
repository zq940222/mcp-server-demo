# 动态工具发现故障排查

## 问题：工具已发现但未显示

### 症状
- ✅ 日志显示：`✅ Dynamically discovered 1 tools for toolset: example-tools`
- ❌ 客户端未显示任何工具

### 根本原因

Spring AI MCP Server 在**连接建立时**就已经完成了工具扫描，之后动态注册的Bean无法被发现。

### 解决方案

#### 方案1: 确保Bean在正确时机注册（当前实现）

我们已经实现了：
1. `DynamicToolsetInterceptor` - 在请求处理前注册工具
2. `DynamicToolsetRegistry` - 动态创建和注册工具Bean
3. `McpToolRegistrar` - 尝试直接注册到MCP Server

**问题**：Spring AI MCP Server可能在拦截器执行之前就已经扫描了工具。

#### 方案2: 使用BeanDefinition注册（推荐尝试）

修改 `registerToolBean` 方法，使用 `BeanDefinition` 而不是 `registerSingleton`：

```java
private void registerToolBean(String beanName, Object instance, Class<?> toolClass) {
    ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
    
    // Create BeanDefinition
    org.springframework.beans.factory.support.GenericBeanDefinition beanDefinition = 
        new org.springframework.beans.factory.support.GenericBeanDefinition();
    beanDefinition.setBeanClass(toolClass);
    beanDefinition.setScope(org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON);
    
    // Register BeanDefinition
    ((org.springframework.beans.factory.support.DefaultListableBeanFactory) beanFactory)
        .registerBeanDefinition(beanName, beanDefinition);
    
    // Register singleton instance
    beanFactory.registerSingleton(beanName, instance);
}
```

#### 方案3: 在启动时预注册（临时方案）

如果动态发现无法工作，可以在启动时预注册所有工具，但使用条件控制：

```java
@Bean
@ConditionalOnProperty(name = "mcp.toolset.preload", havingValue = "true")
public ExampleTools exampleTools() {
    // ...
}
```

#### 方案4: 使用Spring AI MCP Server的运行时API（如果可用）

检查是否有运行时注册API：

```java
@Autowired(required = false)
private McpSyncServer mcpSyncServer;

public void registerTools(List<Object> toolInstances) {
    if (mcpSyncServer != null) {
        // Use runtime API
        for (Object toolInstance : toolInstances) {
            // Convert to tool specification and register
        }
    }
}
```

## 调试步骤

### 1. 检查Bean是否注册

添加日志确认Bean已注册：

```java
log.info("Registered beans: {}", Arrays.toString(beanFactory.getBeanDefinitionNames()));
log.info("Singleton beans: {}", beanFactory.getSingletonNames());
```

### 2. 检查MCP Server扫描

查看Spring AI MCP Server的日志，确认它扫描了哪些Bean。

### 3. 检查工具方法

确认工具类有 `@McpTool` 注解的方法：

```java
Method[] methods = toolInstance.getClass().getMethods();
for (Method method : methods) {
    if (AnnotationUtils.findAnnotation(method, McpTool.class) != null) {
        log.info("Found @McpTool method: {}", method.getName());
    }
}
```

### 4. 检查Bean名称

Spring AI MCP Server可能只扫描特定名称模式的Bean。尝试使用标准的Bean命名约定：

```java
// 使用类名的首字母小写形式
String beanName = Character.toLowerCase(className.charAt(0)) + className.substring(1);
```

## 当前状态

- ✅ 工具类已正确扫描和发现
- ✅ 工具实例已动态创建
- ✅ Bean已注册到Spring容器
- ❌ Spring AI MCP Server未发现动态注册的Bean

## 下一步

1. **检查Spring AI MCP Server的扫描机制**：查看它如何扫描Bean
2. **尝试BeanDefinition方式**：使用BeanDefinition而不是registerSingleton
3. **检查MCP Server初始化时机**：确保在MCP Server初始化之前注册工具
4. **考虑使用事件机制**：触发MCP Server重新扫描

## 临时解决方案

如果动态发现无法实现，可以：
1. 在启动时注册所有工具（使用条件控制）
2. 在工具方法中检查toolset参数
3. 不匹配时返回错误

这虽然不是真正的动态发现，但可以实现运行时过滤。

