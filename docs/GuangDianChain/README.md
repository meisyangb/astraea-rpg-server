# GuangDianChain

> 光点连锁系统 — 连锁挖矿/砍树/范围破坏/耐久消耗

---

## 一、简介

GuangDianChain 提供连锁挖掘功能(一键挖矿/砍树)，支持潜行切换、工具限制和耐久消耗。

---

## 二、命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/chain toggle` | 无 | 开关连锁 |
| `/chain reload` | `guangdian.chain.admin` | 重载 |

---

## 三、配置

```yaml
# 连锁最大方块数
max-blocks: 64
# 允许的工具
tools:
  - NETHERITE_PICKAXE
  - NETHERITE_AXE
# 每方块耐久消耗
durability-cost: 1
```

---

*最后更新: 2026-06-13*
