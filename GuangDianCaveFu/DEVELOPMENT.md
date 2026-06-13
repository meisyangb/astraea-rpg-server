# 空岛洞府插件 - 开发需求文档

## 一、项目概述

### 1.1 项目名称
**GuangDianCaveFu** (光点洞府)

### 1.2 项目描述
类似地皮系统的洞府插件，玩家拥有独立的空中空间，可升级扩展，支持多人共建和权限管理。

### 1.3 核心特性
- 独立洞府空间，互不可见
- 可升级系统（4x4 → 更大）
- 多人共建，权限管理
- 快捷回家传送
- 区域保护（防破坏）

---

## 二、空间设计方案

### 2.1 世界结构
- 创建独立的洞府世界 `CaveFuWorld`
- 虚空世界（无地形生成）
- 洞府悬浮在虚空中

### 2.2 视距隔离方案
```
服务端配置：
- 洞府世界视距：2 chunks (32格)
- 玩家进入洞府时自动限制视距
- 离开洞府恢复正常视距

空间分配：
- 基础间隔：48格（确保视距内只有自己的洞府）
- 最大洞府大小：32x32（预留扩展空间）
- 网格分配坐标
```

### 2.3 洞府布局示意
```
Y轴视角（俯视）：

每48格分配一个洞府位

    (0,0)      (48,0)      (96,0)
      ┌──┐       ┌──┐       ┌──┐
      │A1│       │A2│       │A3│ ...
      └──┘       └──┘       └──┘

    (0,48)     (48,48)
      ┌──┐       ┌──┐
      │B1│       │B2│  ...
      └──┘       └──┘

每个格子 = 48x48 预留空间
实际洞府大小按等级决定（4x4、8x8等）
洞府居中放置在预留空间内
```

### 2.4 Y轴高度
```
Y = 64: 洞府底部基岩平台
Y = 65-80: 洞府可用空间（按等级调整高度）
Y = 81+: 开放天空
```

---

## 三、等级与升级系统

### 3.1 等级配置 (levels.yml)

| 等级 | 名称 | 大小 | 高度 | 升级需求 |
|------|------|------|------|----------|
| 1 | 初级洞府 | 4x4 | 6格 | 无（初始） |
| 2 | 中级洞府 | 8x8 | 10格 | 灵石x10, 钻石x5 |
| 3 | 高级洞府 | 12x12 | 16格 | 灵石x50, 仙玉x5 |
| 4 | 仙府 | 16x16 | 24格 | 仙玉x20, 天晶x3 |
| 5 | 神府 | 24x24 | 32格 | 天晶x10, 神石x1 |
| 6 | 至尊神府 | 32x32 | 48格 | 神石x5 |

### 3.2 升级物品格式
```yaml
# 支持两种格式
upgrade-cost:
  - "rpgitem:灵石:10"      # RPGItems物品
  - "vanilla:DIAMOND:5"    # 原版物品
  - "rpgitem:仙玉:5"
```

### 3.3 升级流程
1. 玩家手持升级物品
2. 执行 `/cave upgrade` 命令
3. 检测物品是否满足需求
4. 扣除物品，扩展洞府边界
5. 新增区域生成基岩平台

---

## 四、权限管理系统

### 4.1 权限等级

| 权限 | 描述 | 能力 |
|------|------|------|
| **OWNER** | 洞主 | 全部权限 + 成员管理 + 升级 |
| **MEMBER** | 成员 | 建造、破坏、使用容器、传送 |
| **VISITOR** | 访客 | 仅参观，不可交互 |

### 4.2 权限操作

| 操作 | 命令 | 描述 |
|------|------|------|
| 添加成员 | `/cave invite <玩家>` | 邀请玩家成为成员 |
| 移除成员 | `/cave kick <玩家>` | 移除玩家权限 |
| 查看成员 | `/cave members` | 查看所有成员列表 |
| 离开洞府 | `/cave leave` | 成员主动离开 |
| 转让洞主 | `/cave transfer <玩家>` | 转让洞主身份 |

### 4.3 权限存储结构
```yaml
caves:
  "玩家UUID":
    owner: "玩家名"
    level: 1
    members:
      "成员UUID":
        name: "成员名"
        permission: "MEMBER"
        joinTime: 1234567890
```

---

## 五、区域保护系统

### 5.1 保护范围
- 洞府边界内（根据等级大小）
- Y轴范围：基岩平台到天空

### 5.2 保护规则

| 行为 | OWNER | MEMBER | VISITOR | 其他玩家 |
|------|-------|--------|---------|----------|
| 破坏方块 | ✅ | ✅ | ❌ | ❌ |
| 放置方块 | ✅ | ✅ | ❌ | ❌ |
| 使用容器 | ✅ | ✅ | ❌ | ❌ |
| 使用门/开关 | ✅ | ✅ | ❌ | ❌ |
| 攻击生物 | ✅ | ✅ | ❌ | ❌ |
| 传送进入 | ✅ | ✅ | ❌ | ❌ |

### 5.3 保护实现
```java
// 仅需监听3个事件
@EventHandler
public void onBreak(BlockBreakEvent e) {
    // 检查是否在洞府内
    // 检查是否有权限
    // 无权限则取消事件
}

@EventHandler
public void onPlace(BlockPlaceEvent e) {
    // 同上逻辑
}

@EventHandler
public void onInteract(PlayerInteractEvent e) {
    // 同上逻辑
}
```

---

## 六、命令系统

### 6.1 玩家命令 `/cave`

| 命令 | 描述 | 权限节点 |
|------|------|----------|
| `/cave create` | 申请创建洞府 | guangdian.cave.create |
| `/cave home` | 传送回洞府 | guangdian.cave.home |
| `/cave sethome` | 设置传送点 | guangdian.cave.sethome |
| `/cave info` | 查看洞府信息 | guangdian.cave.info |
| `/cave upgrade` | 升级洞府 | guangdian.cave.upgrade |
| `/cave invite <玩家>` | 邀请成员 | guangdian.cave.invite |
| `/cave kick <玩家>` | 移除成员 | guangdian.cave.kick |
| `/cave members` | 成员列表 | guangdian.cave.members |
| `/cave leave` | 离开洞府 | guangdian.cave.leave |
| `/cave transfer <玩家>` | 转让洞主 | guangdian.cave.transfer |
| `/cave visit <玩家>` | 访问他人洞府 | guangdian.cave.visit |

### 6.2 管理命令 `/caveadmin`

| 命令 | 描述 | 权限节点 |
|------|------|----------|
| `/caveadmin tp <玩家>` | 传送至玩家洞府 | guangdian.cave.admin |
| `/caveadmin delete <玩家>` | 删除玩家洞府 | guangdian.cave.admin |
| `/caveadmin setlevel <玩家> <等级>` | 设置洞府等级 | guangdian.cave.admin |
| `/caveadmin reload` | 重载配置 | guangdian.cave.admin |
| `/caveadmin list` | 查看所有洞府 | guangdian.cave.admin |

---

## 七、配置文件

### 7.1 config.yml
```yaml
# 洞府世界设置
world:
  name: "CaveFuWorld"
  view-distance: 2        # 洞府内视距
  grid-size: 48           # 洞府间隔
  base-y: 64              # 基础Y轴高度

# 消息配置
messages:
  prefix: "&6[洞府] &f"
  no-permission: "&c你没有权限执行此操作！"
  cave-created: "&a洞府创建成功！"
  cave-full: "&c洞府空间已满，请联系管理员！"
  already-have-cave: "&c你已经拥有洞府了！"
  upgrade-success: "&a洞府升级成功！等级: {level}"
  upgrade-failed: "&c升级失败，材料不足！"
  invite-sent: "&a已向 {player} 发送邀请！"
  invite-received: "&e{player} 邀请你加入洞府，输入 /cave accept 接受"
  not-owner: "&c你不是洞主！"
  player-not-member: "&c该玩家不是你的洞府成员！"

# 初始设置
settings:
  default-level: 1        # 初始等级
  max-members: 10         # 最大成员数
```

### 7.2 levels.yml
```yaml
levels:
  1:
    name: "初级洞府"
    size: 4
    height: 6
    upgrade-cost: []
  2:
    name: "中级洞府"
    size: 8
    height: 10
    upgrade-cost:
      - "rpgitem:灵石:10"
      - "vanilla:DIAMOND:5"
  3:
    name: "高级洞府"
    size: 12
    height: 16
    upgrade-cost:
      - "rpgitem:灵石:50"
      - "rpgitem:仙玉:5"
  4:
    name: "仙府"
    size: 16
    height: 24
    upgrade-cost:
      - "rpgitem:仙玉:20"
      - "rpgitem:天晶:3"
  5:
    name: "神府"
    size: 24
    height: 32
    upgrade-cost:
      - "rpgitem:天晶:10"
      - "rpgitem:神石:1"
  6:
    name: "至尊神府"
    size: 32
    height: 48
    upgrade-cost:
      - "rpgitem:神石:5"
```

---

## 八、数据存储

### 8.1 存储方式
- 使用 YAML 文件存储
- 文件路径：`plugins/GuangDianCaveFu/data.yml`

### 8.2 数据结构
```yaml
caves:
  "uuid-xxxx-xxxx":
    id: 1                           # 洞府ID（用于计算位置）
    owner: "玩家名"
    owner-uuid: "uuid-xxxx"
    level: 2
    world: "CaveFuWorld"
    center-x: 24                    # 中心坐标
    center-z: 24
    home-y: 66                      # 传送点Y
    created: 1234567890
    members:
      "member-uuid-1":
        name: "成员1"
        permission: "MEMBER"
        join-time: 1234567890
      "member-uuid-2":
        name: "成员2"
        permission: "MEMBER"
        join-time: 1234567891

# 用于快速分配下一个洞府位置
next-cave-id: 100
```

---

## 九、项目结构

```
GuangDianCaveFu/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/cn/guangdian/cavefu/
    │   │   ├── GuangDianCaveFu.java        # 主类
    │   │   ├── cave/
    │   │   │   ├── Cave.java               # 洞府数据模型
    │   │   │   ├── CaveManager.java        # 洞府管理
    │   │   │   └── CaveLevel.java          # 等级配置
    │   │   ├── permission/
    │   │   │   ├── PermissionType.java     # 权限类型枚举
    │   │   │   └── PermissionManager.java  # 权限管理
    │   │   ├── protection/
    │   │   │   └── ProtectionListener.java # 区域保护监听
    │   │   ├── world/
    │   │   │   ├── CaveWorldManager.java   # 世界管理
    │   │   │   └── VoidGenerator.java      # 虚空生成器
    │   │   ├── upgrade/
    │   │   │   └── UpgradeManager.java     # 升级管理
    │   │   ├── command/
    │   │   │   ├── CaveCommand.java        # 玩家命令
    │   │   │   └── CaveAdminCommand.java   # 管理命令
    │   │   └── storage/
    │   │       └── DataManager.java        # 数据存储
    │   └── resources/
    │       ├── plugin.yml
    │       ├── config.yml
    │       └── levels.yml
    └── test/
```

---

## 十、依赖关系

### 10.1 必需依赖
- Spigot/Paper API 1.21.4

### 10.2 可选依赖
- PlaceholderAPI（变量支持）
- MythicMobs（自定义物品支持）

### 10.3 PlaceholderAPI 变量
| 变量 | 描述 |
|------|------|
| `%gdcave_owner%` | 洞主名称 |
| `%gdcave_level%` | 洞府等级 |
| `%gdcave_size%` | 洞府大小 |
| `%gdcave_members%` | 成员数量 |
| `%gdcave_has_cave%` | 是否拥有洞府 |

---

## 十一、开发计划

### Phase 1: 基础框架
- [ ] 项目结构搭建
- [ ] 配置系统
- [ ] 数据模型

### Phase 2: 核心功能
- [ ] 世界生成（虚空世界）
- [ ] 洞府创建
- [ ] 区域保护

### Phase 3: 权限系统
- [ ] 成员管理
- [ ] 权限检查

### Phase 4: 升级系统
- [ ] 等级配置
- [ ] 升级逻辑
- [ ] MythicMobs物品支持

### Phase 5: 命令与传送
- [ ] 玩家命令
- [ ] 管理命令
- [ ] 传送功能

### Phase 6: 优化与测试
- [ ] 视距限制
- [ ] 性能优化
- [ ] 功能测试

---

## 十二、待确认事项

1. **洞府创建是否需要消耗物品？** 目前设计为免费创建，如需消耗请告知具体物品。

2. **成员数量上限是否按等级变化？** 目前统一设置为10人，是否需要等级越高成员越多？

3. **是否需要访客功能？** 即允许非成员进入参观但不可操作。

4. **洞府删除后物品如何处理？** 直接清空还是尝试返还？

5. **是否需要洞府聊天频道？** 类似工会聊天。

---

**请审核以上需求文档，确认或提出修改意见。**