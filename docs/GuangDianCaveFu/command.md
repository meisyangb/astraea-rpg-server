# GuangDianCaveFu 命令权限完整参考

## 权限节点

| 权限节点 | 说明 |
|---------|------|
| `guangdian.cavefu.admin` | 管理员权限(重载、设置等级、删除洞府) |

## 命令详细说明

### `/cave create` — 创建洞府
- **权限**: 无
- **检查**: 玩家是否已有洞府(每人限1个)
- **效果**: 创建虚空世界、生成洞府区域

### `/cave home` — 传送回家
- **权限**: 无
- **要求**: 已创建洞府

### `/cave upgrade` — 升级洞府
- **权限**: 无(需为洞主)
- **消耗**: 当前等级对应的 upgrade-cost

### `/cave invite <玩家>` — 邀请成员
- **权限**: 无(需为洞主)
- **限制**: 成员数未达上限

### `/cave kick <玩家>` — 踢出成员
- **权限**: 无(需为洞主)

### `/cave leave` — 离开洞府
- **权限**: 无
- **限制**: 洞主不能离开(需先转让)

### `/cave transfer <玩家>` — 转让所有权
- **权限**: 无(需为洞主)
- **目标**: 必须已是成员

### `/cave sethome` — 设置传送点
- **权限**: 无(需为洞主)

### `/cave info [玩家]` — 洞府信息
- **权限**: 无
- **显示**: 等级、大小、成员数、成员列表

### `/caveadmin setlevel <玩家> <等级>` — 设置等级
- **权限**: `guangdian.cavefu.admin`

### `/caveadmin delete <玩家>` — 删除洞府
- **权限**: `guangdian.cavefu.admin`

### `/caveadmin reload` — 重载配置
- **权限**: `guangdian.cavefu.admin`

### `/caveadmin info` — 系统统计
- **权限**: `guangdian.cavefu.admin`
- **显示**: 洞府总数、存储模式、是否有待保存数据

### `/caveadmin tp <玩家>` — 传送到洞府
- **权限**: `guangdian.cavefu.admin`

---

*最后更新: 2026-06-13*
