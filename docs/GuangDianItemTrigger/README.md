# GuangDianItemTrigger

> 物品触发系统 - 右键/左键物品触发动作

---

## 一、简介

通过物品 Lore 中的关键词匹配，触发指定动作（命令、消息、音效、消耗物品等）。

### 功能特性
- 右键/左键触发
- 潜行触发
- Lore 关键词匹配
- 多种动作（命令、消息、音效、消耗、金币）
- 冷却系统

---

## 二、配置文件 (`config.yml`)

```yaml
triggers:
  # 黄金出售
  少量黄金:
    enabled: true
    trigger-type: right_click
    lore-keyword: "价值1000金"
    actions:
      - "vault_money:1000"
    cooldown: 0

  点券1000:
    enabled: true
    trigger-type: right_click
    lore-keyword: "兑换1000点券"
    actions:
      - "message:<green>你成功兑换了 <yellow>1000 <green>点券!"
      - "console:points give %player_name% 1000"
      - "sound:minecraft:entity.player.levelup:1.0:1.0"
      - "take:1"
    cooldown: 0
```

---

## 三、触发类型

| 类型 | 说明 |
|:----|:-----|
| `right_click` | 右键点击 |
| `left_click` | 左键点击 |
| `shift_right_click` | 潜行+右键 |
| `shift_left_click` | 潜行+左键 |
| `on_hit` | 攻击命中 |
| `consume` | 消耗物品 |

---

## 四、动作类型

| 动作 | 格式 | 说明 |
|:----|:-----|------|
| `vault_money:<金额>` | `vault_money:1000` | 获得金币 |
| `message:<文本>` | `message:<green>成功!` | 发送消息 |
| `console:<命令>` | `console:points give %player_name% 1000` | 控制台命令 |
| `sound:<音效>` | `sound:minecraft:entity.player.levelup:1.0:1.0` | 播放音效 |
| `take:<数量>` | `take:1` | 消耗物品 |
| `effect:<效果>` | `effect:HEAL:1:1` | 药水效果 |

---

*最后更新: 2026-06-13*
