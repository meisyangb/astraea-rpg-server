# GuangDianCustomModels 插件

## 简介

GuangDianCustomModels 是一个完全独立的自定义武器贴图和模型插件，无需依赖任何外部插件。它能够：

- 自动扫描贴图目录（10000+贴图）
- 自动生成2D/3D模型JSON文件
- 自动生成标准Minecraft资源包
- 自动分配CustomModelData
- 支持自定义物品属性
- 提供完整的命令系统

## 安装

1. 将 `GuangDianCustomModels-1.0.0.jar` 放入服务器 `plugins` 目录
2. 启动服务器，插件会自动生成配置文件
3. 编辑 `config.yml` 配置贴图目录路径
4. 重启服务器或使用 `/custommodels reload` 命令

## 配置

### config.yml

```yaml
textures:
  source_directory: "e:/RPG/10000+贴图/贴图/贴图7"  # 贴图目录
  output_directory: "plugins/GuangDianCustomModels/output"
  auto_scan: true
  categories:
    - "SD"   # 剑类
    - "AXE"  # 斧类
    - "BOW"  # 弓类
    - "SPR"  # 矛类
    - "STF"  # 法杖类
    - "DGR"  # 匕首类

models:
  generation_mode: "2d"  # 2d 或 3d

resource_pack:
  pack_format: 48  # Minecraft 1.21.6
  pack_name: "GuangDian_CustomModels"
  auto_generate_on_startup: false
```

### items.yml

定义自定义物品的属性、名称、贴图等。

## 使用流程

### 步骤1：扫描贴图

```bash
/custommodels scan
```

扫描贴图目录，分析贴图分类和状态。

### 步骤2：生成模型

```bash
/custommodels generate
```

为所有贴图生成对应的模型JSON文件（2D或3D模式）。

### 步骤3：打包资源包

```bash
/custommodels pack
```

生成完整的资源包ZIP文件，包含：
- pack.mcmeta
- 所有贴图文件
- 所有模型JSON文件

### 步骤4：安装资源包

将生成的资源包安装到客户端：
1. 复制 `GuangDian_CustomModels.zip` 到客户端 `resourcepacks` 目录
2. 在游戏中启用资源包

### 步骤5：获取物品

```bash
/custommodels give sd_1sd100003
```

获取自定义物品。

## 命令列表

| 命令 | 描述 | 权限 |
|------|------|------|
| `/custommodels scan` | 扫描贴图目录 | custommodels.admin |
| `/custommodels generate` | 生成模型JSON | custommodels.admin |
| `/custommodels pack` | 打包资源包 | custommodels.admin |
| `/custommodels list` | 列出所有物品 | custommodels.use |
| `/custommodels list <ID>` | 查看物品详情 | custommodels.use |
| `/custommodels give <ID> [数量]` | 给予物品 | custommodels.give |
| `/custommodels reload` | 重载配置 | custommodels.admin |
| `/custommodels info` | 查看插件信息 | custommodels.use |
| `/custommodels help` | 显示帮助 | custommodels.use |

## 技术特性

### 完全独立

- 不依赖RPGCore或其他任何插件
- 标准Bukkit插件架构
- 使用Paper API 1.21.6

### 自动化处理

- 自动贴图分类（SD、AXE、BOW等）
- 自动模型生成（2D/3D）
- 自动资源包打包
- 自动CustomModelData分配

### 扩展性

- 支持自定义物品属性
- 支持自定义显示名称和Lore
- 支持自定义模型模板
- 支持多种材质类型

## 贴图分类规则

插件会根据贴图文件名自动分类：

| 分类关键词 | 武器类型 | 默认材质 |
|-----------|---------|---------|
| SD | 剑类 | DIAMOND_SWORD |
| AXE | 斧类 | DIAMOND_AXE |
| BOW | 弓类 | BOW |
| SPR | 矛类 | DIAMOND_SWORD |
| STF | 法杖类 | BLAZE_ROD |
| DGR | 匕首类 | DIAMOND_SWORD |
| BingFaShi | 冰法武器 | DIAMOND_SWORD |
| FaShi | 法师武器 | BLAZE_ROD |
| KuangZhanShi | 狂战武器 | DIAMOND_AXE |
| WuShi | 武士武器 | DIAMOND_SWORD |
| YanShuShi | 岩术武器 | BLAZE_ROD |
| YuWei | 御卫武器 | DIAMOND_SWORD |

## 模型生成模式

### 2D模式（快速部署）

- 平面模型，适合快速测试
- 使用 `item/generated` 父类
- 仅设置贴图路径

### 3D模式（视觉效果）

- 立体模型，视觉效果更好
- 包含完整的显示参数
- 支持手持视角调整

## 资源包格式

- **pack_format**: 48（Minecraft 1.21.6）
- **目录结构**:
  ```
  assets/guangdian/
    textures/weapons/  # 贴图文件
    models/item/       # 模型JSON
  ```

## 开发信息

- **版本**: 1.0.0
- **API**: Paper 1.21.6
- **Java**: JDK 21
- **构建工具**: Gradle 9.4.0

## 构建方法

```bash
cd e:\原创RPG服务端
set JAVA_HOME=e:\原创RPG服务端\tools\jdk-21.0.10+7
D:\gradle\gradle-9.4.0\bin\gradle.bat build --no-configuration-cache -x test
```

生成的JAR文件位于：`plugins/GuangDianCustomModels/build/libs/GuangDianCustomModels-1.0.0.jar`

## 注意事项

1. **CustomModelData范围**：从10000开始，避免与原版物品冲突
2. **资源包安装**：必须在客户端启用资源包才能看到自定义模型
3. **贴图格式**：仅支持PNG、JPG、JPEG格式
4. **材质类型**：必须使用有效的Minecraft材质

## 未来计划

- [ ] 支持更多武器类型
- [ ] 支持装备贴图
- [ ] 支持动态模型（弓拉弓状态）
- [ ] 支持粒子效果
- [ ] 支持技能系统集成

## 技术支持

如遇问题，请检查：
1. 贴图目录路径是否正确
2. 贴图文件格式是否支持
3. 资源包是否已安装到客户端
4. CustomModelData是否冲突

---

**GuangDian Team** - 2026