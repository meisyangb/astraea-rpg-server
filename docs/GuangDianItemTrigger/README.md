# GuangDianItemTrigger

> 光点物品触发器 — 右键/左键/潜行右键触发命令/技能/消息

---

## 一、简介

GuangDianItemTrigger 让自定义物品拥有交互触发能力，支持右键/左键/潜行右键触发命令、技能和消息。

---

## 二、命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/itemtrigger reload` | `guangdian.itemtrigger.admin` | 重载配置 |
| `/itemtrigger list` | 无 | 查看触发物品 |

---

## 三、配置

```yaml
triggers:
  recall_scroll:
    item-id: "rpg:传送卷轴"
    trigger-type: RIGHT_CLICK
    cooldown: 300
    commands:
      - "console:warp spawn {player}"
    messages:
      - "<green>已使用传送卷轴"
```

---

*最后更新: 2026-06-13*
