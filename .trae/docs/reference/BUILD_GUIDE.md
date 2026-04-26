# Astraea RPG 唯一构建方法

> 本文档定义了项目唯一的构建流程，所有构建必须严格按照此方法执行
> 
> **版本**: 2.0.0
> **创建日期**: 2026-04-10
> **更新日期**: 2026-04-26
> **状态**: 唯一标准

---

## ⚠️ 重要说明

**此文档是唯一有效的构建方法，任何其他构建脚本或方法均已废弃。**

---

## 📋 构建环境要求

| 项目 | 值 |
|------|---|
| JDK | JDK 21 (`tools/jdk-21.0.10+7`) |
| Gradle | 9.4.0 (`D:\gradle\gradle-9.4.0`) |
| 项目根目录 | `e:\原创RPG服务端` |

---

## 🔧 唯一构建命令

### 1. 环境设置

```powershell
cd e:\原创RPG服务端
$env:JAVA_HOME="e:\原创RPG服务端\tools\jdk-21.0.10+7"
```

### 2. 构建所有插件

```powershell
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" build --no-configuration-cache -x test
```

### 3. 清理并构建

```powershell
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" clean build --no-configuration-cache -x test
```

### 4. 构建单个插件

```powershell
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" :plugins:RPGCore:build --no-configuration-cache -x test
```

---

## 📁 项目结构

```
e:\原创RPG服务端/
├── settings.gradle          # Gradle 多项目配置
├── build.gradle             # 根构建配置
├── plugins/                 # 所有插件源码
│   ├── RPGCore/
│   │   ├── build.gradle     # 插件构建配置
│   │   ├── libs/            # 本地依赖
│   │   │   ├── paper-api.jar
│   │   │   ├── PlaceholderAPI.jar
│   │   │   ├── LuckPerms.jar
│   │   │   ├── Vault.jar
│   │   │   └── ProtocolLib.jar
│   │   └── build/libs/      # 构建输出
│   ├── GuangDianArmorStats/
│   ├── GuangDianPoints/
│   └── ... (其他插件)
└── server/                  # 服务器目录
    └── plugins/             # 部署目标
```

---

## 📦 依赖配置规范

### 根 build.gradle

```gradle
// Astraea RPG - 根构建配置
plugins {
    id 'java'
}

allprojects {
    group = 'cn.guangdian'
    version = '1.0.0'
    
    repositories {
        mavenCentral()
        maven { url = 'https://repo.papermc.io/repository/maven-public/' }
    }
}

subprojects {
    apply plugin: 'java'
    
    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    compileJava {
        options.encoding = 'UTF-8'
    }
}
```

### 插件 build.gradle 模板

```gradle
plugins {
    id 'java'
}

repositories {
    flatDir {
        dirs 'libs'
    }
}

dependencies {
    // Paper API (网络)
    compileOnly 'io.papermc.paper:paper-api:1.21.6-R0.1-SNAPSHOT'
    
    // RPGCore (项目依赖)
    compileOnly project(':plugins:RPGCore')
    
    // 本地依赖
    compileOnly files('libs/Vault.jar')
    compileOnly files('libs/LuckPerms.jar')
}

jar {
    archiveBaseName.set('插件名')
}
```

---

## 🗂️ 本地依赖文件

以下 JAR 文件必须存在于 `plugins/RPGCore/libs/` 目录：

| 文件名 | 来源 | 用途 |
|--------|------|------|
| `paper-api.jar` | PaperMC | Paper API |
| `PlaceholderAPI.jar` | server/plugins | 占位符 API |
| `LuckPerms.jar` | server/plugins | 权限 API |
| `Vault.jar` | server/plugins | 经济 API |
| `ProtocolLib.jar` | server/plugins | 协议库 |

### 复制依赖命令

```powershell
# 从 server/plugins 复制依赖到 RPGCore/libs
cd e:\原创RPG服务端\server\plugins

# PlaceholderAPI
Copy-Item "[变量]PlaceholderAPI.jar" "..\..\plugins\RPGCore\libs\PlaceholderAPI.jar" -Force

# LuckPerms
Copy-Item "[权限]LuckPerms.jar" "..\..\plugins\RPGCore\libs\LuckPerms.jar" -Force

# Vault
Copy-Item "[经济]VaultUnlocked.jar" "..\..\plugins\RPGCore\libs\Vault.jar" -Force

# ProtocolLib
Copy-Item "ProtocolLib.jar" "..\..\plugins\RPGCore\libs\ProtocolLib.jar" -Force

# Paper API
Copy-Item "..\paper-1.21.6.jar" "..\..\plugins\RPGCore\libs\paper-api.jar" -Force
```

---

## ✅ 构建验证

### 检查构建输出

```powershell
Get-ChildItem -Path "plugins" -Recurse -Filter "*.jar" -File | 
    Where-Object { $_.FullName -like "*build\libs*" } | 
    Select-Object Name, Length
```

### 预期输出

```
RPGCore-1.0.0.jar           ~700KB
GuangDianArmorStats-1.0.0.jar  ~80KB
GuangDianPoints-1.0.0.jar      ~58KB
... (其他插件)
```

---

## 🚫 禁止事项

1. **禁止使用其他构建脚本** - 如 `build-all.ps1` 已废弃
2. **禁止跳过环境设置** - 必须设置 `JAVA_HOME`
3. **禁止修改构建配置** - 除非经过审核
4. **禁止提交依赖 JAR** - libs/ 目录已在 .gitignore 中

---

## 📝 构建流程图

```
开始
  ↓
设置 JAVA_HOME
  ↓
检查 libs/ 目录依赖
  ↓
执行 gradle build
  ↓
检查构建输出
  ↓
部署到 server/plugins
  ↓
完成
```

---

## 🔗 相关文档

- [开发规则](.trae/rules/kaifa.md)
- [技能文档](.trae/skills/minecraft-rpg-architect/SKILL.md)
- [版本控制](VERSION_CONTROL.md)

---

*此文档是唯一有效的构建方法*
*任何其他构建脚本均无效*
*最后更新: 2026-04-26*
