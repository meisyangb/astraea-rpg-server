# 共享依赖库

此目录包含所有插件共享的依赖库，避免重复存储。

## 依赖列表

| 文件名 | 大小 | 说明 |
|--------|------|------|
| Vault.jar | 70.6 KB | 经济系统API |
| PlaceholderAPI.jar | 94.3 KB | 占位符API |
| LuckPerms.jar | - | 权限系统 (需在服务器根目录) |
| ProtocolLib.jar | - | 协议库 (需在服务器根目录) |
| MythicMobs.jar | 18.3 MB | 怪物系统 (GuangDianArmorStats专用) |

## 使用说明

在各插件的 `build.gradle` 中使用以下配置引用共享依赖:

```groovy
dependencies {
    compileOnly files('../../libs/Vault.jar')
    compileOnly files('../../libs/PlaceholderAPI.jar')
}
```

## 清理建议

可以安全删除以下重复文件:
- `GuangDianItemTrigger/libs/Vault.jar`
- `GuangDianMarket/libs/Vault.jar`
- `GuangDianMenu/libs/Vault.jar`
- `GuangDianPoints/libs/Vault.jar`
- `GuangDianTab/libs/Vault.jar`
- `GuangDianArmorStats/libs/PlaceholderAPI.jar` (保留MythicMobs.jar)

预计节省空间: ~423 KB (Vault x 6) + 94.3 KB (PlaceholderAPI) = ~517 KB
