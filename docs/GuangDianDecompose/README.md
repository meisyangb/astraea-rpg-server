# GuangDianDecompose

> 光点分解系统 — 装备分解/材料返还/阶位规则/一键分解

---

## 一、简介

GuangDianDecompose 提供装备分解功能，将 RPGItems 装备分解为材料。

---

## 二、命令权限

| 命令 | 权限 | 说明 |
|------|------|------|
| `/decompose` | `guangdian.decompose.use` | 打开分解界面 |
| `/decomposeadmin reload` | `guangdian.decompose.admin` | 重载规则 |
| `/decomposeadmin list` | `guangdian.decompose.admin` | 查看规则列表 |

---

## 三、规则配置 rules.yml

```yaml
rules:
  iron_sword:
    item: "rpg:铁剑"
    materials:
      "rpg:铁锭": 3
      "rpg:木棍": 1
  tier_3:
    tier: 3
    materials:
      "rpg:三阶精华": 2
```

---

*最后更新: 2026-06-13*
