# 项目文件树与职责（0.4.1）

下面列出提交到 GitHub 的项目文件。相同名称的 `fabric.mod.json` 分别属于正式模组和测试模组。

```text
MCmod/
├── README.md                         安装、操作、玩法规则、配置和构建说明
├── CHANGELOG.md                      0.4.0 功能、升级兼容和验证摘要
├── LICENSE                           项目及随附资源的 MIT 许可证
├── .gitignore                        排除缓存、构建产物、游戏存档与本机 API 配置
├── .gitattributes                    统一文本换行，标记图片和 JAR 为二进制
├── build.gradle                      依赖、编译、客户端分离、打包与测试任务
├── settings.gradle                   项目名称与 Gradle 插件仓库
├── gradle.properties                 Minecraft/Fabric/模组版本和构建参数
├── gradlew                           Linux/macOS 构建入口
├── gradlew.bat                       Windows 构建入口
├── gradle/wrapper/
│   ├── gradle-wrapper.jar            启动指定版本 Gradle 的引导程序
│   ├── gradle-wrapper.properties     Gradle 下载地址、版本与校验值
│   └── gradle-8.7-bin.zip.sha256      Gradle 发行包的校验记录
├── src/main/java/dev/thirteenblade/
│   ├── ThirteenBlade.java            公共初始化：注册两把剑与升级配方、事件和怪物强化
│   ├── ThirteenBladeItem.java        剑的基础属性、无耐久、身份初始化和物品提示
│   ├── BalanceConfig.java            读取并校验服务器玩法配置
│   ├── Progression.java              击杀数到等级、下一次升级的纯计算规则
│   ├── BladeData.java                每把剑的 NBT：成长、能力、抢夺效果、冷却和黄心
│   ├── BladeInventory.java           主副手优先级，物品栏最高生命加成
│   ├── BladeGameplay.java            服务器击杀、V 技能、属性刷新及网络同步
│   ├── BladeEffects.java             效果归属、100 刻余效、外部药水兼容与黄心保存
│   ├── BladeOwnedEffect.java         识别剑提供的状态效果的接口
│   ├── SoulPower.java                物种家族到能力的映射、旧蜘蛛刻印兼容
│   ├── DragonUpgradeRecipe.java       基础剑 + 龙蛋，复制原剑自定义数据
│   ├── DragonFlight.java              飞行权限归属、5 秒余效及退出清理
│   ├── EliteMobs.java                新自然怪物的概率强化、属性与药水效果
│   ├── chat/
│   │   ├── ChatSettings.java         本机对话配置与接口地址校验
│   │   ├── ChatMessage.java          一条对话的数据结构
│   │   └── ChatTransport.java        异步 API 请求、超时、历史限制和响应解析
│   └── mixin/
│       ├── PlayerEntityMixin.java    攻击前刷新成长，避免切换后沿用旧伤害
│       ├── LivingEntityMixin.java    饥饿免疫、外部药水优先、移除前保存黄心
│       ├── ClampedAttributeMixin.java  放宽盔甲韧性数值边界，支持 32 点以上
│       ├── MobEntityMixin.java       记录怪物的生成原因
│       └── StatusEffectInstanceMixin.java
│                                     让效果携带并保存“由剑提供”的标记
├── src/client/java/dev/thirteenblade/client/
│   ├── ThirteenBladeClient.java      客户端初始化：V/J、HUD 和网络接收
│   ├── SwordChat.java                每把剑的会话、离线回复、剑灵设定与 API 调用
│   └── SwordChatScreen.java          独立对话窗口、输入、按钮、滚动与属性展示
├── src/main/resources/
│   ├── fabric.mod.json               正式模组的标识、入口、版本及依赖
│   ├── thirteenblade.mixins.json     启用哪些 Mixin
│   ├── assets/thirteenblade/
│   │   ├── lang/zh_cn.json           简体中文
│   │   ├── lang/en_us.json           英文
│   │   ├── models/item/thirteen_blade.json
│   │   │                             手持物品模型与贴图引用
│   │   └── textures/item/thirteen_blade.png
│   │                                 32×32 透明像素贴图
│   └── data/thirteenblade/
│       ├── recipes/thirteen_blade.json
│       │                             工作台合成配方
│       └── advancements/recipes/combat/thirteen_blade.json
│                                     合成配方解锁条件
├── src/test/java/dev/thirteenblade/
│   ├── ProgressionTest.java          成长、旧配置兼容与数值边界的 JUnit 测试
│   └── chat/ChatTransportTest.java   用本机 HTTP 服务验证对话传输
├── src/gametest/
│   ├── java/dev/thirteenblade/
│   │   ├── BladeGameTests.java       耐久、计杀、成长、吸收和饥饿免疫
│   │   ├── SoulGameTests.java        强化怪物、抢夺、效果保存与黄心
│   │   └── CarryGameTests.java       副手、背包生命、多把剑优先级与 5 秒余效
│   └── resources/fabric.mod.json    仅开发测试使用的模组入口，不进入发布 JAR
├── docs/
│   ├── PROJECT_STRUCTURE.md         本文件
│   ├── MOD_DEVELOPMENT_GUIDE.md      面向初学者的模组结构与客户端/服务端讲解
│   ├── TESTING.md                    自动测试覆盖范围及待人工试玩项目
│   └── art/
│       ├── PROMPT.md                美术生成与处理记录
│       ├── sword-source.png         生成的原始美术图
│       └── sword-preview.png        方便查看的放大预览
└── tools/
    ├── prepare_texture.py           美术图到游戏像素贴图的处理工具
    └── validate_resources.py        校验翻译、JSON、贴图和发布 JAR 内容
```

以下是本机运行后生成的目录，不上传 GitHub：

| 目录 | 用途 |
| --- | --- |
| `.git/` | 本地提交历史与远程仓库配置；通过 Git 协议同步提交 |
| `.gradle/`、`.gradle-user-home/` | 构建缓存、下载的游戏与依赖 |
| `build/libs/` | 可安装 JAR 和源码 JAR |
| `build/reports/` | 单元测试报告 |
| `build/gametest/`、`build/gametest-results.xml` | 游戏内测试世界及结果 |
| `run/` | 开发用游戏实例：配置、日志、存档等 |

`src/main` 的“公共”不等于“仅服务端”：其中的物品注册等会在客户端和服务器加载。`src/client` 才是只能由客户端加载的界面、输入等代码。`assets` 负责显示资源，`data` 负责配方等游戏数据。


## 0.4.0 新资源与 NeoForge 独立项目

```text
src/main/resources/
├── assets/thirteenblade/models/item/dragon_thirteen_blade.json  进阶剑物品模型
├── assets/thirteenblade/textures/item/dragon_thirteen_blade.png  龙魂进阶剑透明贴图
└── data/thirteenblade/recipes/dragon_thirteen_blade.json         自定义升级配方类型入口
src/gametest/java/dev/thirteenblade/AscensionGameTests.java      本次进阶与新能力回归测试
neoforge-1.21.1/
├── README.md                      NeoForge 安装、构建、测试和版本要求
├── build.gradle                   ModDevGradle、JDK 21、独立测试源集和运行任务
├── settings.gradle                插件仓库、工具链和项目名称
├── gradle.properties              模组及 NeoForge 版本、JVM 内存
├── gradlew / gradlew.bat           Gradle 9.2.1 启动脚本
├── gradle/wrapper/                Wrapper JAR、下载地址与 SHA-256
├── src/main/java/dev/thirteenblade/
│   ├── BladeNetwork.java          NeoForge 类型化 C2S/S2C 消息和服务器校验
│   ├── client/                    客户端按键、HUD、对话屏幕和会话
│   ├── chat/                      对话配置、消息、HTTP 传输
│   ├── mixin/                     药水归属、免疫、虚弱、攻击与韧性边界
│   └── 其余同名核心类             与 Fabric 对应，使用 Mojang 命名和 NeoForge 事件
├── src/main/resources/
│   ├── META-INF/neoforge.mods.toml 模组入口元数据、依赖范围和 MIT 许可
│   ├── thirteenblade.mixins.json  Java 21 Mixin 列表
│   ├── assets/thirteenblade/      与 Fabric 相同的两把剑模型、贴图、中英文
│   └── data/thirteenblade/        1.21 单数目录 recipe/、advancement/
├── src/test/java/                 成长计算与本机模拟 HTTP 的 JUnit 测试
└── src/gametest/
    ├── java/dev/thirteenblade/BladeGameTests.java   真实服务器行为测试
    └── resources/data/thirteenblade/structure/empty.nbt  测试空结构
```

两个版本分别构建和发布。NeoForge 的 `BladeData` 使用不可变数据组件的更新接口，避免修改 NBT 副本后未写回；`DragonUpgradeRecipe` 复制组件差异，保留进阶剑自己的默认攻击属性。NeoForge 的自然生成检测使用事件，因此不需要 Fabric 的 `MobEntityMixin`。

缓存、下载的 JDK / Minecraft、运行世界和 JAR 构建产物均不提交到源码仓库。

美术新增 `docs/art/dragon-sword-source.png`（生成原图）与 `docs/art/dragon-sword-preview.png`（64×64 游戏贴图的最近邻放大预览）。

0.4.1 新增：两版 `BladeEnchantments.java` 负责固有抢夺 III / 绑定诅咒补齐；两版 `StrengthGameTests.java` 验证升级回血与附魔迁移。`BladeGameplay` 仅在击杀引起等级提升后回血。
