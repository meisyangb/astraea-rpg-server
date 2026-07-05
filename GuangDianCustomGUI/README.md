# GuangDianCustomGUI - 自定义GUI插件

## 功能介绍
- **自定义物品** - 通过资源包 + CustomModelData 实现自定义贴图
- **自定义GUI** - 支持自定义背包界面（可扩展更多GUI）
- **资源包管理** - 玩家登录自动发送资源包
- **MiniMessage支持** - 标题/描述支持现代文本格式

## 安装步骤

### 1. 部署插件
插件已部署到: `e:\RPG\艾尔丽雅\plugins\GuangDianCustomGUI.jar`

### 2. 配置资源包
编辑 `e:\RPG\艾尔丽雅\plugins\GuangDianCustomGUI\config.yml`:

```yaml
resource-pack:
  enabled: true
  # 修改为实际的HTTP服务器地址
  url: "http://你的服务器IP:8080/resourcepack.zip"
  send-on-join: true
```

### 3. 启动资源包HTTP服务器
```bash
python e:\RPG\start_resourcepack_server.py
```

### 4. 重启Minecraft服务器
插件会自动加载并发送资源包给玩家。

## 命令
- `/customgui backpack` - 打开自定义背包
- `/customgui reload` - 重新加载配置 (需要权限)
- `/customgui resourcepack send [url]` - 发送资源包

## 权限
- `customgui.backpack` - 打开自定义背包
- `customgui.reload` - 重新加载配置
- `customgui.resourcepack.send` - 发送资源包

## 资源包结构
```
resourcepack.zip
── pack.mcmeta
├── pack.png
└── assets/minecraft/
    ├── textures/item/
    │   ├── custom_backpack.png       # 背包背景 (256x256)
    │   └── custom_backpack_border.png # 边框
    ── models/item/
        ├── gray_stained_glass_pane.json  # CustomModelData映射
        ├── custom_backpack.json
        ── custom_backpack_border.json
```

## 自定义材质
- 背包背景: CustomModelData 10001
- 背包边框: CustomModelData 10002

## 开发
- 源代码: `e:\RPG\原创RPG服务端-插件最多提交\plugins\GuangDianCustomGUI`
- 构建: `gradle build`
- 部署: `gradle deploy`
