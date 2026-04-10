---
id: build-process
date: 2026-04-10
type: pattern
category: build
importance: critical
---

# Astraea RPG 唯一构建流程

> 此记忆文件记录了项目唯一的构建方法，每次构建必须严格按照此流程执行

---

## 构建命令 (唯一)

```powershell
# 1. 设置环境
cd e:\原创RPG服务端
$env:JAVA_HOME="e:\原创RPG服务端\tools\jdk-21.0.10+7"

# 2. 构建所有插件
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" build --no-configuration-cache -x test
```

---

## 依赖配置

### 本地依赖位置
```
plugins/RPGCore/libs/
├── paper-api.jar
├── PlaceholderAPI.jar
├── LuckPerms.jar
├── Vault.jar
└── ProtocolLib.jar
```

### 复制依赖命令
```powershell
cd e:\原创RPG服务端\server\plugins
Copy-Item "[变量]PlaceholderAPI.jar" "..\..\plugins\RPGCore\libs\PlaceholderAPI.jar" -Force
Copy-Item "[权限]LuckPerms.jar" "..\..\plugins\RPGCore\libs\LuckPerms.jar" -Force
Copy-Item "[经济]VaultUnlocked.jar" "..\..\plugins\RPGCore\libs\Vault.jar" -Force
Copy-Item "ProtocolLib.jar" "..\..\plugins\RPGCore\libs\ProtocolLib.jar" -Force
```

---

## 构建配置文件

### settings.gradle (根目录)
```gradle
rootProject.name = 'astraea-rpg-server'
include('plugins:RPGCore', 'plugins:GuangDianArmorStats', ...)
```

### build.gradle (根目录)
```gradle
allprojects {
    group = 'cn.guangdian'
    version = '1.0.0'
    repositories {
        mavenCentral()
        maven { url = 'https://repo.papermc.io/repository/maven-public/' }
    }
}
```

### build.gradle (插件模板)
```gradle
plugins {
    id 'java'
}

repositories {
    flatDir { dirs 'libs' }
}

dependencies {
    compileOnly 'io.papermc.paper:paper-api:1.21.6-R0.1-SNAPSHOT'
    compileOnly project(':plugins:RPGCore')
    compileOnly files('libs/Vault.jar')
}

jar {
    archiveBaseName.set('插件名')
}
```

---

## 构建输出

```
plugins/{插件名}/build/libs/{插件名}-1.0.0.jar
```

---

## 注意事项

1. **禁止使用其他构建脚本** - `build-all.ps1` 已废弃
2. **必须设置 JAVA_HOME** - JDK 21
3. **依赖文件必须存在** - libs/ 目录
4. **跳过测试** - 使用 `-x test` 参数

---

*创建时间: 2026-04-10*
*状态: 唯一标准*
