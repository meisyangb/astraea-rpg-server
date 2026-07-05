# GuangDianBoard

> 光点计分板 — 自定义 Scoreboard/动态更新/PlaceholderAPI 变量/条件显示

---

## 一、简介

GuangDianBoard 提供高度自定义的计分板系统，支持 PlaceholderAPI 变量和条件显示。

---

## 二、命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/board reload` | `guangdian.board.admin` | 重载 |
| `/board toggle` | 无 | 开关计分板 |

---

## 三、配置

```yaml
boards:
  default:
    title: "<gold>⚔ 译梦传说"
    lines:
      - "<gray>──────────"
      - "<white>等级: %rpgcore_level%"
      - "<white>点券: %gdpoints_balance%"
      - "<gray>──────────"
      - "<yellow>在线: %server_online%"
```

---

*最后更新: 2026-06-13*
