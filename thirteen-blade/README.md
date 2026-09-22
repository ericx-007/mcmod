# 十三契刃 / Thirteen Blade

一个增加成长剑、灵魂抢夺及随机强化怪物的 Fabric 模组原型，当前版本 **0.3.0**，支持 **Minecraft Java 1.20.1、Java 17、Fabric Loader 0.16.14 或更高版本**。客户端和服务器都需要安装相同版本的本模组及对应版本的 Fabric API。物品种类仍只新增一把剑。

开源协议：[MIT License](LICENSE)。

首次开发模组，可以先读 [结合本项目的开发入门讲解](docs/MOD_DEVELOPMENT_GUIDE.md)。

查看源码时可以对照 [完整文件树与各文件作用](docs/PROJECT_STRUCTURE.md)。

![剑的像素贴图](docs/art/sword-preview.png)

## 开始试玩

1. 在启动器中创建 **Minecraft 1.20.1 + Fabric** 实例。官方启动器可以使用 [Fabric 安装器](https://fabricmc.net/use/installer/)，也可以使用启动器自带的 Fabric 安装功能。
2. 将 `build/libs/thirteen-blade-0.3.0.jar` 放入该实例的 `mods` 文件夹。更新时移出旧版 JAR，避免同时安装两个版本；不要安装 `-sources.jar`。旧剑的击杀数和原有刻印可继续读取。
3. 同时安装 [Fabric API 0.92.2+1.20.1](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.92.2+1.20.1/fabric-api-0.92.2+1.20.1.jar)。Fabric Loader 和 Fabric API 是两个组件，详见 [Fabric 安装说明](https://wiki.fabricmc.net/install)。
4. 建议先进入新的创造模式世界，在「战斗用品」中取出「十三契刃」，或开启作弊后执行：

```mcfunction
/give @s thirteenblade:thirteen_blade
```

生存合成：工作台中间一列，从上到下依次摆放 **紫水晶碎片 → 钻石剑 → 末影珍珠**。这是普通合成配方，输入钻石剑的附魔、自定义名称和耐久信息不会继承。

| 操作 | 默认按键 |
| --- | --- |
| 开启 / 取消灵魂吸收 | V |
| 打开独立剑灵对话框 | J |
| 发送对话 / 返回游戏 | Enter / Esc |
| 翻阅对话 | 鼠标滚轮 |

按键可以在「选项 → 控制 → 按键绑定 → 十三契刃」中修改。V、J、HUD、成长攻击和刻印能力支持主手或副手持剑，双手都有剑时主手优先。生命加成只要求剑留在物品栏。对话窗口不会暂停游戏。

## 剑的成长与平衡

- 初始基础攻击 **6 点**，攻击速度 **1.6 次/秒**；正常的蓄力、护甲、暴击和附魔规则仍然生效。
- 不消耗耐久，掉落物防火。并非不会丢失：仍受掉落物消失、虚空等规则影响。
- 每 **13 次有效击杀**提升一级，每级 **+2 攻击、+2 最大生命值**。2 点生命等于 1 颗心。
- **取消模组的等级上限**：130 次击杀为 10 级，143 次为 11 级，1300 次为 100 级。100 级时主手基础攻击为 206 点、携带时最大生命为 220 点（不含其他来源加成）。旧配置中的 `maxLevel` 已不再读取，旧剑自动按全部击杀数计算等级。
- 击杀数、已吸收能力和冷却保存在每一把剑的 NBT 上，保存世界、换主人后仍保留。两把剑分别成长。
- **生命加成**：快捷栏、背包普通格子或副手中携带剑即可生效；多把剑取最高加成，不相加。不读取箱子、末影箱、背包中的潜影盒内部或掉落物。最后一把剑离开物品栏、死亡或进入旁观模式时撤销；降低上限时截断超额当前生命，携带和切换不直接回血。
- **攻击与击杀归属**：主手或副手握剑时启用成长攻击；双手都持剑时只使用主手那把。副手模式仍由主手执行原版近战，剑的成长攻击加到这次攻击上，击杀计入副手剑；不额外叠加剑本体的 5 点武器攻击。
- 有效击杀：持有生效剑的玩家以**直接近战伤害**击杀成年生物，包括横扫。玩家、盔甲架、幼年生物、村民 / 流浪商人、已驯服的可驯服宠物不计数。箭、燃烧后续伤害、摔落、宠物代杀不计数。
- 普通刷怪塔 / 刷怪笼生物仍会计数。保留强力成长体验，没有增加反刷怪限制。

物品说明分别列出主手总基础攻击、副手成长攻击和携带生命加成；底部原版属性区显示剑本体的 6 点攻击，成长部分由服务器额外应用。

技术边界：取消的是原先人为设置的 10 级封顶，未修改 Minecraft 原版属性系统。1.20.1 原版最大生命属性仍限制到 1024 点、攻击属性到 2048 点；达到原版边界后击杀和等级继续记录，但对应实际属性会被原版截断。击杀计数使用非负 32 位整数并防止溢出，最多记录 2,147,483,647 次。

## 灵魂吸收

按 V 后进入 **30 秒**的吸收状态，立即开始 **60 秒**冷却。下一次有效击杀消耗吸收机会：

| 目标 | 永久铭刻在该剑上的能力 |
| --- | --- |
| 僵尸及其同类（如尸壳、溺尸） | 任一手持剑时清除并阻止「饥饿」负面效果 |
| 骷髅及其同类（如流浪者、凋灵骷髅） | 任一手持剑时获得夜视 |
| 蜘蛛 / 洞穴蜘蛛 | 任一手持剑时获得缓降 I |
| 苦力怕 | 任一手持剑时获得伤害吸收 I；再次技能击杀可补满黄心 |
| 携带增益的有效目标 | 同时夺取其全部非瞬间、正面状态效果 |
| 无上述能力、无可夺取效果的生物 | 本次吸收结束，无新增能力 |

四种固有能力及抢夺的药水增益可共存。被夺取的效果不要求目标必须是强化怪物：其他有效生物身上的正面增益也可以夺取。负面效果（如中毒）及瞬间效果（如瞬间治疗）不会被夺取。同种效果保留较高等级，默认最高 III，不按击杀次数叠级。

固有能力永久铭刻；抢夺的增益默认也永久记录在剑上，并以真正的无限时长状态效果在任一手持剑时生效。收剑后剑提供的药水效果转为 **100 游戏刻（通常约 5 秒）** 的余效；反复刷新不会延长这次倒计时，原本不足 5 秒的有限效果不会延长。期间重新持剑可恢复刻印效果，死亡后仍随保留下来的剑存在。原版药水、信标等来源的同名效果优先，程序不覆盖它们；等外部效果结束后再应用剑的效果。喝牛奶能清除当前效果，继续持剑会重新获得已铭刻能力。

伤害吸收 I 提供 **4 点吸收生命 / 2 颗黄心**。无限时长不代表无限黄心，黄心受伤后会正常消耗；换剑、重新登录、喝牛奶不会补满。再次技能击杀苦力怕或携带伤害吸收效果的生物可补满该剑当前等级的黄心。每把剑分别保存剩余黄心。

再次按 V、生效剑切换为另一把剑、收进背包、死亡、离线或超时都会结束准备状态，冷却不退还。同一把剑在主副手之间交换不会取消准备。冷却按现实时间计算并保存在剑上，离线时继续流逝。饥饿免疫是持剑时的拦截规则，本身不是药水效果，收剑后立即停止；跑步 / 战斗仍会消耗饱食度。死亡、旁观和离线会立即清理剑提供的药水效果，不保留余效。

## 随机强化怪物

- 新自然生成 / 区块初始生成的成年敌对生物，默认 **5%** 概率成为强化怪物。不额外提高总刷怪数量。
- 生命上限提高到原来的 **1.5 倍**，随机获得 **1～2 种**永久增益，通常为 I～II 级。
- 效果池：速度、力量、抗性提升、生命恢复、抗火、伤害吸收。抗火固定 I 级。
- 强化怪物带发光轮廓和药水粒子，沿用原版模型和 AI，仍按原版规则自然消失。
- 刷怪笼、刷怪蛋、命令生成、幼年怪物、凋灵、末影龙和监守者不参与此随机强化。
- 每个自然生成的怪物只判定一次，保存重载不重新抽取，也不重复增加生命。
- 这是怪物生成规则改动，不是地形生成改动。在旧存档的已探索区域，新刷出的自然怪物同样可能强化；已存在的怪物不会被批量改造。

## 与剑灵对话

J 打开的窗口完全独立于多人聊天。默认是离线关键词回应，可以询问「你是谁」「如何吸收」「成长」「饥饿」。这不是本地大模型。

API 模式支持 **Chat Completions 兼容协议**，例如提供该接口的云服务或本机模型服务。接口格式参考 [Chat Completions 文档](https://api-docs.deepseek.com/api/create-chat-completion/)。第一版使用非流式响应。

首次启动客户端后，编辑游戏实例内的 `config/thirteenblade-chat.json`（开发运行时位于 `run/config/`）：

```json
{
  "enabled": true,
  "endpoint": "https://你的服务域名/v1/chat/completions",
  "model": "填写服务商实际提供的模型名",
  "apiKeyEnvironmentVariable": "THIRTEEN_BLADE_API_KEY",
  "apiKey": "",
  "timeoutSeconds": 30,
  "maxTokens": 240,
  "temperature": 0.8
}
```

- `endpoint` 是完整接口地址，程序不会自动添加路径。云服务要求 HTTPS；本机 `localhost`、`127.0.0.1`、`[::1]` 可使用 HTTP，例如 `http://localhost:11434/v1/chat/completions`。
- 推荐在本机环境变量中设置 `THIRTEEN_BLADE_API_KEY`，然后重新启动游戏 / 启动器。也可以在本机 JSON 的 `apiKey` 字段填写密钥；不要分享含密钥的配置文件。环境变量优先于 JSON。
- 不需要认证的本机服务可以留空密钥。配置中的示例域名和模型名只是占位，不会默认连接任何真实服务。
- 保存后重新打开 J 窗口，或点击「读取配置」。配置无效时退回离线模式；网络或服务异常时给出提示并使用离线回应。
- 只有玩家点击发送才调用 API，每次发送最多 512 字符，同一把剑同时仅一个请求，并有 1.5 秒发送间隔。
- 外发内容为当前剑的属性、最近至多 20 条对话及剑灵设定；不发送玩家姓名、坐标、服务器地址或多人聊天。使用服务商 API 可能按其规则计费。
- 密钥不写入剑的 NBT、不发给游戏服务器、不写入日志。对话仅保存在本次连接的客户端内存里，按剑区分；断开世界清空，最多保留 16 把剑的会话。
- API 只产生文字，不执行游戏命令、改变属性或生成物品。

## 修改数值

编辑游戏 / 服务器实例内的 `config/thirteenblade.json`，然后重新启动。单机使用本机配置，多人服以服务器配置为准并同步到客户端。

```json
{
  "killsPerLevel": 13,
  "damagePerLevel": 2.0,
  "healthPerLevel": 2.0,
  "absorptionWindowSeconds": 30,
  "absorptionCooldownSeconds": 60,
  "eliteSpawnChance": 0.05,
  "eliteHealthMultiplier": 1.5,
  "eliteMaxEffects": 2,
  "eliteMaxEffectLevel": 2,
  "maxStolenEffectLevel": 3,
  "stolenEffectDurationSeconds": -1
}
```

修改升级阈值或每级加成后，会按原有击杀数重新计算成长，不删除击杀记录。当前程序对配置数值设置合理范围，避免负值、无限值或除零错误。

`eliteSpawnChance` 范围为 0～1，0 关闭强化，1 代表符合条件的新怪物必定强化。`stolenEffectDurationSeconds = -1` 表示新夺取的增益永久保留；正整数表示从抢夺成功起的现实秒数，离线及收剑也继续计时。修改时长只影响之后的抢夺，已存的永久效果不追溯缩短。固定的四种物种刻印不受这个时长选项影响。旧版配置缺失的新字段自动采用默认值，可以手动将这些字段加入原 JSON。

## 构建与开发

在项目根目录使用 JDK 17：

```powershell
.\gradlew.bat --gradle-user-home .gradle-user-home build
.\gradlew.bat --gradle-user-home .gradle-user-home runClient
```

macOS / Linux 可使用 `bash ./gradlew --gradle-user-home .gradle-user-home build`。首次运行需要下载 Gradle、Minecraft 和 Fabric 依赖；客户端运行还需下载游戏资源。

构建输出为 `build/libs/thirteen-blade-0.3.0.jar`。项目固定使用 Gradle 8.7、Loom 1.6.12、Yarn 1.20.1+build.10、Loader 0.16.14、Fabric API 0.92.2+1.20.1。

主要入口：

- `src/main/java/dev/thirteenblade/BladeGameplay.java`：服务器击杀、吸收、属性及同步。
- `src/main/java/dev/thirteenblade/BladeData.java`：每把剑的持久化状态。
- `src/main/java/dev/thirteenblade/BladeInventory.java`：主副手选择、物品栏最高生命加成。
- `src/main/java/dev/thirteenblade/BladeEffects.java`：效果归属、收剑 5 秒余效、外部药水兼容、黄心保存。
- `src/main/java/dev/thirteenblade/EliteMobs.java`：新自然怪物的概率强化。
- `src/client/java/dev/thirteenblade/client/SwordChatScreen.java`：客户端对话界面。
- `src/main/java/dev/thirteenblade/chat/ChatTransport.java`：异步 API 传输和响应限制。
- `src/main/resources/assets/thirteenblade/lang/`：简体中文及英文。
- `src/main/resources/assets/thirteenblade/textures/item/thirteen_blade.png`：32×32 透明贴图。

自动测试与尚需人工验证的内容见 [验收清单](docs/TESTING.md)。

## 美术

内置 imagegen 生成钻石剑轮廓、紫青色泰拉瑞亚风格的原创像素剑，再用最近邻采样整理成 32×32 游戏贴图。没有提取或打包泰拉瑞亚原版材质。

原图、像素预览、生成提示词分别见 `docs/art/sword-source.png`、`docs/art/sword-preview.png`、[美术记录](docs/art/PROMPT.md)。

## 开源协议

本项目的原创源码、文档和美术资源采用 [MIT License](LICENSE)：允许使用、修改、分发及商用，分发副本或实质性部分时须保留版权声明和许可声明。软件按原样提供，不作担保。正式条款以 `LICENSE` 中的英文全文为准。

第三方组件（例如 Gradle Wrapper、Minecraft 和 Fabric 相关依赖）遵循各自的许可证，本项目的 MIT 声明不替代它们的许可条款。
