# Astraea RPG 项目命名规范

> 本项目名称: **AstraeaRPG** (GitHub仓库名)
> 中文名: 阿斯特瑞亚 RPG 服务端
> 命名空间: `cn.guangdian.*`

---

## 1. GitHub 仓库命名

### 推荐命名
```
astraea-rpg-server
```

### 备选命名
- `astraea-minecraft-rpg` - 更明确的Minecraft标识
- `guangdian-rpg-core` - 强调GuangDian插件体系
- `astraea-rpg-paper` - 强调Paper服务端

### 命名规则
- 使用小写字母
- 单词间用连字符 `-` 分隔
- 避免使用下划线 `_`
- 保持简洁明了

---

## 2. 项目结构命名

```
astraea-rpg-server/           # 仓库根目录
├── .trae/                    # AI助手配置
│   ├── rules/                # 规则文件
│   ├── skills/               # 技能文件
│   └── knowledge/            # 知识库
├── plugins/                  # 插件源码
│   ├── RPGCore/              # 核心插件
│   ├── GuangDianName/        # 命名插件
│   └── ...
├── server/                   # 服务端文件
│   └── plugins/              # 编译后的插件
├── tools/                    # 工具链
│   └── jdk-21.0.10+7/        # JDK
├── docs/                     # 文档
├── .gitignore                # Git忽略文件
├── README.md                 # 项目说明
└── build.gradle              # 构建配置
```

---

## 3. 插件命名规范

### Java 包名
```
cn.guangdian.[插件名]
```

示例:
- `cn.guangdian.rpgcore` - RPGCore核心
- `cn.guangdian.name` - GuangDianName
- `cn.guangdian.points` - GuangDianPoints

### 插件主类命名
```java
[插件名]Plugin.java
```

示例:
- `RPGCorePlugin.java`
- `NamePlugin.java`
- `PointsPlugin.java`

### 服务接口命名
```java
[功能]Service.java
```

示例:
- `PointsService.java`
- `NameService.java`
- `DisplayService.java`

---

## 4. Git 分支命名

### 主分支
- `main` - 主分支 (稳定版本)
- `develop` - 开发分支

### 功能分支
```
feature/[功能描述]
```

示例:
- `feature/scheduler-migration`
- `feature/new-gui-system`
- `feature/mythicmobs-integration`

### 修复分支
```
fix/[问题描述]
```

示例:
- `fix/bukkit-scheduler-violations`
- `fix/memory-leak`
- `fix/npc-interaction`

### 发布分支
```
release/v[版本号]
```

示例:
- `release/v1.0.0`
- `release/v1.1.0-beta`

---

## 5. Commit 消息规范

### 格式
```
[类型]: [简要描述]

[详细描述]

[关联Issue]
```

### 类型标识
| 类型 | 说明 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat: 添加玩家数据缓存系统` |
| `fix` | 修复 | `fix: 修复Bukkit调度器违规` |
| `docs` | 文档 | `docs: 更新API文档` |
| `style` | 格式 | `style: 统一代码缩进` |
| `refactor` | 重构 | `refactor: 优化数据处理器` |
| `perf` | 性能 | `perf: 优化数据库查询` |
| `test` | 测试 | `test: 添加单元测试` |
| `chore` | 构建 | `chore: 更新Gradle配置` |

### 示例
```
feat: 迁移GuangDianName到RPGCore架构

- 替换Bukkit.getScheduler()为SyncScheduler
- 添加PlayerLifecycleManager集成
- 修复3处getPlugin("RPGCore")违规

Closes #12
```

---

## 6. 版本号规范

### 语义化版本 (SemVer)
```
主版本号.次版本号.修订号
```

示例: `1.2.3`

### 版本规则
- **主版本号**: 破坏性变更、架构升级
- **次版本号**: 新功能、向后兼容
- **修订号**: Bug修复、小优化

### 预发布版本
```
1.0.0-alpha.1
1.0.0-beta.2
1.0.0-rc.1
```

---

## 7. 标签命名

### 版本标签
```
v[版本号]
```

示例:
- `v1.0.0`
- `v1.1.0-beta`

### 创建标签
```bash
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

---

## 8. GitHub 仓库设置建议

### 仓库描述
```
Astraea RPG - Minecraft Paper 1.21.6 RPG服务器插件体系
基于RPGCore架构的高性能RPG插件集合
```

### 主题标签
```
minecraft
paper
rpg
plugin
java
spigot
mythicmobs
```

### README 结构
1. 项目简介
2. 功能特性
3. 技术栈
4. 构建说明
5. 贡献指南
6. 许可证

---

*最后更新: 2026-04-10*
*版本: 1.0.0*
