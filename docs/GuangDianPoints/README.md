# GuangDianPoints

> 点券系统 - 点券货币管理，支持兑换和商店消费

---

## 一、简介

点券货币系统，玩家通过击杀点卷之主、活动奖励等方式获得点券，可在商店兑换稀有物品。

### 功能特性
- 点券获取（击杀Boss、活动、交易）
- 点券消费（商店兑换）
- 点券兑换券物品（右键兑换）
- 点券余额查询
- PlaceholderAPI 支持

---

## 二、点券物品配置

### 点券兑换券 (`items/点卷.yml`)
```yaml
点券兑换:
  Id: PAPER
  Display: '<i:false><yellow>1000<aqua>点券'
  Lore:
  - '<i:false><gray>[<gold>点券<gray>]      <gray>[<light_purple>右键使用<gray>]'
  - '<i:false><green>兑换1000点券'
  - '<i:false><green>使用后消耗掉'
  - '<i:false>'
  - '<i:false><gray>■ 说明'
  - '<i:false><gray>  右键点击即可兑换1000点券'
  - '<i:false><gray>  点券可在商店购买稀有物品'
  - '<i:false><gray>  兑换后物品自动消失'
  ...
```

### ItemTrigger 兑换配置
```yaml
点券1000:
  enabled: true
  trigger-type: right_click
  lore-keyword: "兑换1000点券"
  actions:
    - "message:<green>你成功兑换了 <yellow>1000 <green>点券!"
    - "console:points give %player_name% 1000"
    - "sound:minecraft:entity.player.levelup:1.0:1.0"
    - "take:1"
```

---

## 三、命令权限

| 命令 | 权限 | 说明 |
|------|------|------|
| `/points` | 无 | 查看点券 |
| `/points give <玩家> <数量>` | `points.admin` | 给予点券 |
| `/points take <玩家> <数量>` | `points.admin` | 扣除点券 |
| `/points set <玩家> <数量>` | `points.admin` | 设置点券 |
| `/points pay <玩家> <数量>` | 无 | 转账点券 |

---

*最后更新: 2026-06-13*
