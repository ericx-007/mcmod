# 十三契刃 · Forge 1.12.2

面向 RLCraft 2.9.3 的 Java 8 移植版，玩法版本为 0.4.1。使用 Forge 14.23.5.2847 的开发接口编译，正式 JAR 在 RLCraft 所用 **14.23.5.2860** 上验证。MIT 协议覆盖源码及随附原创贴图。

## 安装

将 `build/libs/thirteen-blade-forge-1.12.2-0.4.1.jar` 放入游戏实例的 `mods` 文件夹。联机时客户端和服务器都要安装同一版本。不要安装 `-dev.jar`、`-sources.jar`、`-integration-tests.jar` 或 `-integration-tests-obf.jar`，不要混入其他加载器或游戏版本的本模组。

V 准备 / 取消下一次吞噬，J 打开私人剑灵对话。在 RLCraft 中如有按键冲突，请在控制设置中重绑。

## 旧版差异

- 1.12.2 没有下界合金和紫水晶：基础配方为工作台中间一列 **下界之星 → 钻石剑 → 末影珍珠**。不继承原钻石剑附魔。基础剑 + 龙蛋无序合成进阶剑，完整继承数据。
- 基础剑最多成长 10 级；进阶剑无本模组等级上限。每 13 次有效近战击杀 +2 攻击、+2 最大生命，实际升级时回满生命。两把剑固有抢夺 III / 绑定诅咒 I，不消耗耐久，注册的专用掉落物防火且可随世界保存。
- 主副手生效、背包最高生命加成、每个新敌对家族 +2 韧性、无限待命的 V、触发后 60 秒冷却、药水与飞行五秒余效、强化怪物和客户端 API 对话均保留。
- 原版可获取 **12 类刻印**：僵尸、骷髅、蜘蛛、苦力怕、烈焰人、凋灵、末影龙、女巫、史莱姆、卫道士、守卫者、潜影贝。此版本没有幻翼、掠夺者、监守者、旋风人，也没有村庄英雄 / 黑暗等新版效果，因此不伪造这些能力。其他模组的敌对实体仍按注册类型增加韧性，并可夺取其持续正面药水效果。
- 旧版没有原生无限药水时长，内部使用长时药水并在持剑期间维护；真正的能力期限保存在剑的 NBT，放下后仍只保留约五秒。黄心剩余量独立保存，不因换剑、喝奶或重登补满。
- 对话界面使用旧版 GUI 与 Java 8 HTTP 接口；保留异步请求、超时、离线回应、每剑历史和响应大小限制。API 配置仍在客户端的 `config/thirteenblade-chat.json`，默认关闭。密钥不发往 Minecraft 服务器。

## RLCraft / First Aid

自动检测 First Aid 1.6.x；没有安装时正常使用原版生命。安装时，剑的生命属性变化会请求 First Aid 按其配置重新计算部位上限，成长升级会恢复**所有身体部位**，并安排同步。保留 First Aid 的 `scaleMaxHealth`、`capMaxHealth` 等规则，不改原整合包配置。

因此“进阶无等级上限”不等于绕过 First Aid、Scaling Health 或原版属性数值边界；整合包仍可限制实际生效生命。副手成长支持不替换 RLCombat 对主手武器基础伤害和掉落附魔的计算。

## 构建与验证

构建进程使用 Java 17，编译 / 游戏测试使用 Java 8。设置 `JAVA_HOME` 指向 JDK 17、`JDK8_HOME` 指向 JDK 8，执行：

```powershell
.\gradlew.bat --gradle-user-home ..\.gradle-user-home-legacy build
```

RetroFuturaGradle 1.4.2 会准备旧版源码与映射；正式输出是无 `-dev` 后缀的 JAR。首次构建需要联网。

可选集成测试：先在本项目 `run` 中接受 Minecraft EULA，再执行：

```powershell
.\gradlew.bat --gradle-user-home ..\.gradle-user-home-legacy -PbladeIntegrationTest runServer
```

测试使用本项目 `run/world`，结束后自动关闭服务器；不要将自己的存档放入此开发目录。结果见 `run/thirteenblade-integration-results.txt`，Gradle 随后检查场景结果，失败则构建失败。测试源码位于 `src/integration`，不会装入正式 JAR。`reobfIntegrationJar` 生成可供正式混淆环境验证的独立测试模组。

完整验证范围和未完成的人工试玩见 [测试记录](../docs/TESTING.md) 与 [整合包适配说明](../docs/MODPACK_COMPATIBILITY.md)。
