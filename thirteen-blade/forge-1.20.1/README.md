# 十三契刃 · Forge 1.20.1

面向 Better MC BMC4 的 Forge 版本，玩法版本 0.4.1，Java 17。按本机整合包的 **Forge 47.4.20** 构建和测试，不需要额外安装 Fabric API 或 NeoForge。

将 `build/libs/thirteen-blade-forge-1.20.1-0.4.1.jar` 放入游戏实例的 `mods`；联机时两端都装同一版本。更新前移出旧 JAR，不要安装 `-sources.jar`，不要同时放入本模组的 Fabric / NeoForge / 1.12.2 版本。

两把剑、下界合金剑基础配方、龙蛋进阶、成长回血、抢夺 III / 绑定诅咒、主副手、背包生命、触发式 V、灵魂韧性、药水抢夺、自然强化怪物及 J 对话，均与现有 Fabric 1.20.1 版对应。具体规则见 [主说明](../README.md)。1.20.1 原版没有旋风人，无法自然获取该刻印；其他 15 类可获取。

控制默认 V / J，整合包已有同键功能时可自行重绑。玩法配置为服务器的 `config/thirteenblade.json`；对话配置和密钥仅保存在客户端 `config/thirteenblade-chat.json`。

## 构建

设置 `JAVA_HOME` 指向 JDK 17：

```powershell
.\gradlew.bat --gradle-user-home ..\.gradle-user-home-forge build
.\gradlew.bat --gradle-user-home ..\.gradle-user-home-forge runGameTestServer
```

使用 ForgeGradle 6.0.54、MixinGradle 0.7.38、Gradle 8.7。正式 JAR 已重新混淆并包含 Mixin refmap。单元测试与 GameTest 独立于正式输出。

16 项单元测试与 17 项 GameTest 已通过。完整整合包验证范围见 [适配记录](../docs/MODPACK_COMPATIBILITY.md)，客户端贴图、GUI、按键冲突和真实多人操作仍需试玩。
