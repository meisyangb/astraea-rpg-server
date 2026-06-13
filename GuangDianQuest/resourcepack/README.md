# GuangDianQuest 资源包配置

## 使用 book.png 作为 GUI 背景

### 1. 资源包目录结构

```
GuangDianQuest_ResourcePack/
├── pack.mcmeta
├── pack.png
└── assets/
    └── minecraft/
        └── models/
            └── item/
                └── paper.json
```

### 2. pack.mcmeta

```json
{
  "pack": {
    "pack_format": 34,
    "description": "GuangDianQuest GUI 资源包"
  }
}
```

> pack_format: 34 = Minecraft 1.21

### 3. paper.json (CustomModelData 配置)

```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "item/book"
  },
  "overrides": [
    {
      "predicate": {
        "custom_model_data": 1001
      },
      "model": "item/book_background"
    }
  ]
}
```

### 4. 创建 book_background.json

在 `assets/minecraft/models/item/` 目录下创建：

```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "gui/book"
  }
}
```

### 5. 安装资源包

1. 将资源包放入服务器 `resource_packs/` 目录
2. 或让玩家下载放入客户端 `resourcepacks/` 目录
3. 在服务器配置中强制加载资源包 (server.properties)

```properties
# server.properties
resource-pack=https://your-domain.com/GuangDianQuest_ResourcePack.zip
resource-pack-sha1=<SHA1校验和>
require-resource-pack=true
```

### 6. 插件配置 (config.yml)

```yaml
gui:
  background-material: PAPER
  background-custom-model-data: 1001
  use-custom-background: true
```

## 原版材质路径

- 书本 GUI 背景: `assets/minecraft/textures/gui/book.png`
- 书本图标: `assets/minecraft/textures/item/book.png`

## 注意事项

1. CustomModelData 范围建议: 1000-9999 (避免与其他插件冲突)
2. 资源包需要玩家客户端加载才能显示
3. 如果资源包未加载，将显示默认 PAPER 材质
