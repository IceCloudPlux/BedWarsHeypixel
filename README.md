# BedWarsHeypixel - 基于BedWars1058二次开发的新型插件

## 前言

本插件经过原作者 andrei1058 授权，添加了诸多功能和内容，每个月至少更新1~3次。

---

## 功能介绍（对于原版1058）

### 【道具部分】

#### ----= 新增 =----

- [√] 救援平台
- [√] 回城卷轴
- [√] 防御墙
- [√] 隐身药水（防止隐身根本看不见，我们为其添加了更大的粒子效果）
- [×] 其他功能，在日后更新添加

#### ----= 修改 =----

- [√] 烈焰弹配置修改（完全按照模组源码还原爆炸半径与击退手感）

### 【商店部分】

#### ----= 修改 =----

- [√] Q键添加/去除快捷购买，Shift+点击购买一组

### 【模式部分】

#### ----= 新增 =----

- [√] 经验模式
- [√] 经验商店配置
- [√] 经验队伍增益配置
- [√] 经验转换配置
- [√] 游戏开局副标题显示
- [√] 10~6秒标题显示
- [√] debug调试模式下时间缩减
- [√] 4、5级钻石以及绿宝石资源点级别
- [√] 胜利后显示用时
- [√] 变量 {team}

#### ----= 修改 =----

- [√] 取消资源生成速率限制
- [√] 取消挂机惩罚机制
- [√] 计分板队伍状态显示（有床/团灭）
- [√] 等级显示优化（阶数/星级）

---

## 使用说明

### 环境要求

| 项目 | 要求 |
| --- | --- |
| 服务端 | Spigot / Paper 及其兼容核心 |
| 版本支持 | Minecraft **1.8.8 ~ 1.20.4**（含 1.20.1） |
| 运行环境 | **Java 11** 或更高版本 |

可选软依赖（不装也能运行，装上可解锁扩展功能）：

- **Vault**：经济系统
- **PlaceholderAPI**：聊天/计分板变量
- **Citizens**：加入游戏 NPC
- **Parties / PartyAndFriends / Spigot-Party-API-PAF**：队伍系统
- **SlimeWorldManager / AdvancedWorldManager**：更快的世界地图加载与还原

### 安装与授权

1. 将插件 jar 文件放入服务端的 `plugins` 文件夹。
2. 启动一次服务端，让插件生成配置文件和语言文件，然后停止服务端。
3. 编辑 `plugins/BedWarsHeypixel/config.yml`：
   - 在 `license-key` 中填入你的**授权密钥**（本插件为付费授权插件，无有效密钥无法启动）。
   - 根据你的服务器规模设置 `serverType`（`MULTIARENA` / `SHARED` / `BUNGEE`）。
   - 设置默认 `language` 语言。
4. 重新启动服务端，等待授权验证通过后插件即可正常启用。
   - 授权密钥为空、无效或校验失败时，插件会输出错误日志并自动禁用。
5. 使用 `/bw setLobby` 设置主大厅，再按下方"竞技场搭建"创建地图。

> 提示：每次修改配置文件后可使用 `/bw reload` 热重载，无需重启服务端。

### 配置文件

插件首次启动后会在 `plugins/BedWarsHeypixel/` 下生成以下配置：

| 文件 | 说明 |
| --- | --- |
| `config.yml` | 主配置：服务器类型、语言、授权密钥、数据库、游戏时间与倒计时、开关选项等 |
| `shop.yml` | 常规商店配置（含快捷购买默认槽位） |
| `shopxp.yml` | 经验商店配置（经验模式专用） |
| `upgrades2.yml` / `upgrades3.yml` | 队伍升级配置 |
| `generators.yml` | 资源生成器配置（铁、金、钻石、绿宝石生成速度与等级） |
| `signs.yml` | 加入游戏牌子配置 |
| `levels.yml` | 等级经验配置（升级所需经验、奖励等） |
| 语言文件夹 | 各语言的提示消息（支持 /bw lang 切换） |

### 常用命令

主命令为 `/bw`（别名 `/bedwars`、`/bedwars1058`）。

**玩家命令**

| 命令 | 说明 |
| --- | --- |
| `/bw cmds` | 查看所有可用命令 |
| `/bw join <地图/组名/random>` | 加入指定地图、地图组或随机加入 |
| `/bw leave` | 离开当前游戏 |
| `/bw gui <组名>` | 打开竞技场选择 GUI |
| `/bw lang` | 切换个人语言 |
| `/bw stats` | 查看个人战绩 |
| `/bw teleporter` | 打开观战传送菜单 |
| `/bw tp <玩家>` | 传送到指定玩家 |

**管理命令（需对应权限）**

| 命令 | 说明 | 权限 |
| --- | --- | --- |
| `/bw setLobby` | 设置主大厅出生点 | `bw.setup` |
| `/bw start` / `/bw forceStart` | 强制开始游戏（带 `debug` 参数可进入调试模式并缩短开局倒计时） | `bw.forcestart` |
| `/bw time add <秒>` | 为当前游戏增加时间 | `bw.*` |
| `/bw level setLevel <玩家> <等级>` | 设置玩家等级 | `bw.level` |
| `/bw level giveXp <玩家> <经验>` | 给玩家添加经验 | `bw.level` |
| `/bw reload` | 热重载全部配置文件 | `bw.reload` |
| `/bw setupArena <名称>` | 进入竞技场搭建模式 | `bw.setup` |
| `/bw arenaList` | 查看所有竞技场列表 | `bw.setup` |
| `/bw delArena <名称>` | 删除竞技场 | `bw.delete` |
| `/bw enableArena <名称>` | 启用竞技场 | `bw.enableRotation` |
| `/bw disableArena <名称>` | 禁用竞技场 | `bw.disable` |
| `/bw cloneArena <源> <目标>` | 复制竞技场 | `bw.clone` |
| `/bw arenaGroup <组名> [add/remove]` | 管理竞技场分组 | `bw.groups` |
| `/bw build` | 切换建造模式（脱离游戏保护范围） | `bw.build` |
| `/bw npc` | 创建加入游戏 NPC（需安装 Citizens） | `bw.npc` |

**竞技场搭建命令（进入 `/bw setupArena` 后在对应世界内使用）**

| 命令 | 说明 |
| --- | --- |
| `/bw autoCreateTeams <数量>` | 自动创建指定数量的队伍 |
| `/bw createTeam <颜色>` / `/bw removeTeam <颜色>` | 创建 / 移除队伍 |
| `/bw setSpawn <颜色>` | 设置队伍出生点 |
| `/bw setBed <颜色>` | 设置队伍床的位置 |
| `/bw setShop <队伍颜色>` | 设置队伍商店位置 |
| `/bw setUpgrade <队伍颜色>` | 设置队伍升级 NPC 位置 |
| `/bw setWaitingSpawn` | 设置等待大厅出生点 |
| `/bw waitingPos` | 设置等待大厅位置 |
| `/bw setSpectSpawn` | 设置观战出生点 |
| `/bw setMaxInTeam <数量>` | 设置队伍最大人数 |
| `/bw setMaxBuildHeight <高度>` | 设置最大建造高度 |
| `/bw addGenerator <类型>` / `/bw removeGenerator <类型>` | 添加 / 移除资源生成器 |
| `/bw setType <类型>` | 设置竞技场类型（Solo/4v4 等） |
| `/bw gameMode <模式>` | 设置游戏模式 |
| `/bw setKillDrops` | 设置击杀掉落物位置 |
| `/bw save` | 保存竞技场 |

### 权限列表

| 权限 | 说明 |
| --- | --- |
| `bw.*` | 所有权限 |
| `bw.setup` | 搭建 / 管理竞技场 |
| `bw.reload` | 重载配置 |
| `bw.forcestart` | 强制开始游戏 |
| `bw.build` | 游戏内建造权限 |
| `bw.delete` | 删除竞技场 |
| `bw.clone` | 复制竞技场 |
| `bw.enableRotation` / `bw.disable` | 启用 / 禁用竞技场 |
| `bw.groups` | 管理竞技场分组 |
| `bw.npc` | 创建加入 NPC |
| `bw.level` | 管理玩家等级经验 |
| `bw.rejoin` | 断线重连权限 |
| `bw.shout` | 全服喊话 |
| `bw.vip` | 加入满员游戏（将非 VIP 玩家踢出） |
| `bw.chatcolor` | 聊天颜色权限 |
| `bw.cmd.bypass` | 命令绕过权限 |

### 经验模式说明

- 在 `config.yml` 中开启经验模式后，游戏内拾取的资源（铁锭、金锭、钻石、绿宝石）会按照**经验转换配置**的比例自动转换为经验。
- 玩家可在 **经验商店**（`shopxp.yml`）中使用经验购买物品。
- 队伍升级包含**经验队伍增益配置**，通过队伍经验提升全队增益效果。
- 经验模式同样支持击杀、胜利等行为获得经验，具体奖励可在 `levels.yml` 中调整。

### 新道具玩法说明

- **救援平台**：在悬空时使用，脚下生成 5×5 镂空十字粘液块平台，可抵消跌落伤害，平台持续 15 秒后消失，且不可被破坏。
- **回城卷轴**：使用后进入 5 秒回城倒计时（伴有粒子特效），期间移动或丢弃物品会取消，倒计时结束后传送回队伍出生点。
- **防御墙**：在面前生成 3×5 的砂岩墙体，支持斜向放置；前方被遮挡时会自动回退到身后生成。
- **隐身药水**：提供隐身效果，并添加了更大的粒子效果，避免"隐身根本看不见"的问题。

### 常见问题（FAQ）

**Q：插件启动时报错 "No license key configured"？**
A：请在 `config.yml` 的 `license-key` 中填写有效的授权密钥，并重启服务端。

**Q：修改了配置需要重启服务端吗？**
A：不需要，直接使用 `/bw reload` 即可热重载全部配置。

**Q：支持哪些版本？**
A：支持 Minecraft 1.8.8 ~ 1.20.4（含 1.20.1）的 Spigot / Paper 服务端，需要 Java 11+。

**Q：如何快速搭建一个竞技场？**
A：依次执行 `/bw setLobby` → `/bw setupArena <名称>` → `/bw autoCreateTeams <数量>` → 在世界中按搭建命令设置出生点、床、商店、资源生成器等 → `/bw save` → `/bw enableArena <名称>`。

---

> 本项目基于 [BedWars1058](https://github.com/andrei1058/BedWars1058)（GNU GPL v3）二次开发，由 andrei1058 授权。
