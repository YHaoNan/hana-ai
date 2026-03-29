# 日志系统设计

## 1. 设计目标

作为被集成到其他系统的 Library，日志系统需要：
- **零运行时依赖** - 不强制集成方使用特定日志框架
- **灵活配置** - 集成方完全控制日志实现和配置
- **标准化 API** - 使用业界通用的日志接口

## 2. 技术选型

### SLF4J (Simple Logging Facade for Java)

- **API 层面**: `slf4j-api` - 仅定义接口，无实现
- **测试绑定**: `slf4j-simple` - 简单实现，用于开发测试
- **集成方负责**: 提供实际的日志实现（Logback、Log4j2、JUL 等）

### 自封装 Logger 类

提供 `AppLogger` 工具类，简化 SLF4J 使用：

```java
// 获取 Logger
AppLogger log = AppLogger.get(MyClass.class);

// 使用
log.debug("message: {}", arg);
log.info("info message");
log.warn("warning: {}", detail);
log.error("error", exception);
```

## 3. 使用规范

### 日志级别

| 级别 | 使用场景 |
|------|----------|
| DEBUG | 详细流程信息、参数值、中间变量 |
| INFO | 重要业务事件、状态变化 |
| WARN | 可恢复的异常、潜在问题 |
| ERROR | 异常失败、业务中断 |

### 组件日志点

#### OpenAIChatModel
- `INFO`: 初始化、注册 Tool
- `DEBUG`: 发送请求、接收响应、Tool Call 事件
- `ERROR`: 请求失败、解析错误

#### SimpleChatModelAgent
- `INFO`: 启动、完成
- `DEBUG`: Turn 流程、Tool 执行、Memory 操作
- `WARN`: 重复 Tool 调用
- `ERROR`: 执行失败

## 4. 集成方配置

集成方可通过 classpath 添加任意 SLF4J 实现：

```xml
<!-- Maven -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.4.11</version>
</dependency>
```

默认使用 `slf4j-simple`，输出到 System.err，可通过以下系统属性配置：
```properties
# slf4j-simple 配置
org.slf4j.simpleLogger.defaultLogLevel=info
org.slf4j.simpleLogger.showDateTime=true
org.slf4j.simpleLogger.dateTimeFormat=HH:mm:ss.SSS
```

## 5. 模块职责

```
hana-ai (Library)
├── slf4j-api (仅 API)
├── AppLogger (工具类)
├── OpenAIChatModel (日志点)
├── SimpleChatModelAgent (日志点)
└── ... 其他组件

集成方应用
├── logback-classic / log4j2 / ...
├── 日志配置文件
└── 业务代码 + hana-ai
```
