# 十三契刃 — NeoForge 1.21.1

与 [Fabric 版](../README.md) 相同的 0.4.0 玩法，包括两把剑、升级继承、触发吞噬、16 类物种能力、韧性、精英怪和独立 API 对话。此版本支持原版旋风人和沼骸。

## 安装

- Minecraft Java **1.21.1**、Java **21**、NeoForge **21.1.251 或更新的 21.1.x**。
- 客户端与服务器都安装 `thirteen-blade-neoforge-1.21.1-0.4.0.jar`；不装 Fabric API，不与 Fabric 版 JAR 混用。
- 配置：实例内 `config/thirteenblade.json`（玩法）与 `config/thirteenblade-chat.json`（本机对话）。按键 V、J；具体能力及 API 配置见上层 README。
- 此项目是独立加载器适配，不能直接迁移 Fabric 1.20.1 世界存档。NeoForge 物品使用 1.21 的数据组件保存自定义数据。

## 构建

在此目录打开终端，将 `JAVA_HOME` 指向 JDK 21：

```powershell
.\gradlew.bat --gradle-user-home ..\.gradle-user-home-neo build
.\gradlew.bat --gradle-user-home ..\.gradle-user-home-neo runGameTestServer
.\gradlew.bat --gradle-user-home ..\.gradle-user-home-neo runClient
```

Linux/macOS 使用 `bash ./gradlew`。首次构建需联网下载 Gradle、NeoForge、Minecraft 及资源。使用 Gradle 9.2.1、ModDevGradle 2.0.147、官方 Mojang 映射。输出位于 `build/libs/`；`-sources.jar` 仅供阅读源码。

`src/main/java` 同时包含公共逻辑和独立的 `client` 包；客户端入口受 `Dist.CLIENT` 限制，独立服务器不会加载屏幕和按键类。`BladeNetwork` 负责类型化网络消息。`src/gametest` 仅供开发运行，测试类与结构不进入正式发布 JAR。JUnit 在本机模拟 HTTP 服务测试对话传输，不需要真实 API 密钥。

使用 [NeoForge 官方入门](https://docs.neoforged.net/docs/1.21.1/gettingstarted/) 和 [GameTest 文档](https://docs.neoforged.net/docs/1.21.1/misc/gametest/) 中的构建与测试机制。源码、文档和原创美术采用上层 [MIT License](../LICENSE)，发行包内包含许可文本。
