# GuangDianCaveFu

> 空岛洞府系统 - 玩家私人空岛洞府，支持升级、权限管理

---

## 一、简介

玩家私人空岛洞府系统，每位玩家拥有独立洞府空间，支持升级、成员管理、传送点设置。

### 功能特性
- 独立洞府世界（CaveFuWorld）
- 洞府等级升级
- 成员邀请/踢出
- 访客系统
- 传送点设置
- LuckPerms 权限继承
- 领地保护

---

## 二、配置文件 (`config.yml`)

```yaml
# 存储模式
storage:
  use-sqlite: false

# 洞府世界
world:
  name: "CaveFuWorld"
  view-distance: 2
  grid-size: 80

# 基础设置
settings:
  base-y: 64
  default-level: 1
  max-members: 10
  allow-visitor: true

# 创建消耗（免费）
create-cost:
  enabled: false
```

### 等级配置 (`levels.yml`)
```yaml
levels:
  1:
    name: "初级洞府"
    size: 16
    height: 16
    upgrade-cost:
      rpg:灵石: 10
  2:
    name: "中级洞府"
    size: 32
    height: 32
    upgrade-cost:
      rpg:灵石: 50
      rpg:仙玉: 5
  3:
    name: "高级洞府"
    size: 64
    height: 64
```

---

## 三、命令权限

| 命令 | 权限 | 说明 |
|------|------|------|
| `/cave create` | 无 | 创建洞府 |
| `/cave home` | 无 | 回到洞府 |
| `/cave sethome` | `cave.owner` | 设置传送点 |
| `/cave invite <玩家>` | `cave.owner` | 邀请成员 |
| `/cave kick <玩家>` | `cave.owner` | 踢出成员 |
| `/cave leave` | 无 | 离开洞府 |
| `/cave transfer <玩家>` | `cave.owner` | 转让洞主 |
| `/cave upgrade` | `cave.owner` | 升级洞府 |
| `/caveadmin setlevel <玩家> <等级>` | `cave.admin` | 管理员设置等级 |

---

## 四、洞府等级经验需求

| 等级 | 名称 | 大小 | 升级材料 |
|:----:|:----:|:----:|:--------:|
| 1 | 初级洞府 | 16x16 | 灵石 x10 |
| 2 | 中级洞府 | 32x32 | 灵石 x50 + 仙玉 x5 |
| 3 | 高级洞府 | 64x64 | 最高等级 |

---

*最后更新: 2026-06-13*
