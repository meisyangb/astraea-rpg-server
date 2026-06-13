# GuangDianWorldRules 命令列表

## 管理员命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/worldrules` | `guangdianworldrules.admin` | 查看插件信息 |
| `/worldrules reload` | `guangdianworldrules.admin.reload` | 重载配置 |
| `/worldrules info [世界名]` | `guangdianworldrules.admin.info` | 查看世界规则 |
| `/worldrules set <世界> <规则> <值>` | `guangdianworldrules.admin.set` | 设置世界规则 |
| `/worldrules reset <世界>` | `guangdianworldrules.admin.reset` | 重置世界规则 |

## 权限节点

```yaml
guangdianworldrules.admin:
  description: 世界规则管理
  default: op
guangdianworldrules.admin.reload:
  description: 重载配置
  default: op
guangdianworldrules.admin.info:
  description: 查看世界规则
  default: op
guangdianworldrules.admin.set:
  description: 设置世界规则
  default: op
guangdianworldrules.admin.reset:
  description: 重置世界规则
  default: op
```

## 可设置的规则

| 规则名 | 值类型 | 说明 |
|--------|--------|------|
| keep-inventory | true/false | 死亡不掉落物品 |
| keep-exp | true/false | 死亡不掉落经验 |
| disable-natural-spawn | true/false | 禁止自然刷新 |
| disable-monster-spawn | true/false | 禁止怪物刷新 |
| disable-animal-spawn | true/false | 禁止动物刷新 |
| pvp | true/false | PVP开关 |
| disable-explosion-block-damage | true/false | 禁止爆炸破坏 |
| disable-mob-griefing | true/false | 禁止生物破坏 |
