# GuangDianCustomModels 部署指南

## 手动构建方法

由于PowerShell终端限制，请使用以下方法手动构建：

### 方法1：使用IntelliJ IDEA或Eclipse

1. **导入项目**
   - IDE中选择 "Import Project"
   - 选择 `e:\RPG\原创RPG服务端-插件最多提交\plugins\GuangDianCustomModels`
   - 选择 "Gradle" 构建系统

2. **配置JDK**
   - 项目JDK：`e:\RPG\原创RPG服务端-插件最多提交\tools\jdk-21.0.10+7`

3. **构建项目**
   - IDEA: Build > Build Project
   - Eclipse: Project > Build All

4. **生成JAR**
   - IDEA: Build > Build Artifacts > GuangDianCustomModels > Build
   - Eclipse: Export > Java > JAR file

### 方法2：使用CMD命令提示符

1. **打开CMD**（不是PowerShell）
   ```
   Win + R -> cmd -> Enter
   ```

2. **切换目录**
   ```cmd
   cd /d "e:\RPG\原创RPG服务端-插件最多提交"
   ```

3. **设置环境变量**
   ```cmd
   set JAVA_HOME=e:\RPG\原创RPG服务端-插件最多提交\tools\jdk-21.0.10+7
   ```

4. **执行构建**
   ```cmd
   D:\gradle\gradle-9.4.0\bin\gradle.bat build --no-configuration-cache -x test
   ```

### 方法3：使用Maven（转换Gradle项目）

如果Gradle无法使用，可以将项目转换为Maven：

1. 创建 `pom.xml` 文件：
```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>cn.guangdian</groupId>
    <artifactId>GuangDianCustomModels</artifactId>
    <version>1.0.0</version>
    
    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>
    
    <repositories>
        <repository>
            <id>papermc</id>
            <url>https://repo.papermc.io/repository/maven-public/</url>
        </repository>
    </repositories>
    
    <dependencies>
        <dependency>
            <groupId>io.papermc.paper</groupId>
            <artifactId>paper-api</artifactId>
            <version>1.21.6-R0.1-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

2. 执行构建：
```cmd
mvn clean package
```

## 部署到艾尔丽雅服务器

### 步骤1：确认构建结果

构建成功后，JAR文件位于：
```
e:\RPG\原创RPG服务端-插件最多提交\plugins\GuangDianCustomModels\build\libs\GuangDianCustomModels-1.0.0.jar
```

### 步骤2：复制到服务器

手动复制JAR文件到：
```
e:\RPG\艾尔丽雅\plugins\GuangDianCustomModels-1.0.0.jar
```

### 步骤3：启动服务器

1. 启动艾尔丽雅服务器
2. 检查插件加载日志
3. 应看到：
```
========== GuangDianCustomModels 启动 ==========
配置管理器已初始化
贴图管理器已初始化
...
GuangDianCustomModels v1.0.0 已启动!
```

### 步骤4：配置插件

首次启动会自动生成配置文件：
```
e:\RPG\艾尔丽雅\plugins\GuangDianCustomModels\
  config.yml      # 主配置
  items.yml       # 物品定义
```

编辑 `config.yml`：
```yaml
textures:
  source_directory: "e:/RPG/10000+贴图/贴图/贴图7"  # 确认路径正确
```

### 步骤5：使用插件

```bash
/custommodels scan      # 扫描10000+贴图
/custommodels pack      # 生成资源包
/custommodels give <ID> # 获取物品
```

## 快速测试方案

如果无法立即构建，可以使用以下临时方案：

### 方案1：使用其他已构建插件参考

参考其他已构建插件的JAR结构：
- `GuangDianArmorStats-1.0.0.jar`
- `RPGItems-1.0.0.jar`

### 方案2：请求构建协助

将项目交给有Gradle环境的开发者构建：
1. 提供项目目录：`e:\RPG\原创RPG服务端-插件最多提交\plugins\GuangDianCustomModels`
2. 提供JDK路径：`tools\jdk-21.0.10+7`
3. 提供Gradle路径：`D:\gradle\gradle-9.4.0`

## 项目完整性检查

确保所有文件已创建：
```
✅ GuangDianCustomModels.java          # 主类
✅ CustomModelsConfig.java             # 配置管理
✅ TextureManager.java                 # 贴图扫描
✅ ModelGenerator.java                 # 模型生成
✅ ResourcePackGenerator.java          # 资源包打包
✅ CustomItemRegistry.java             # 物品注册
✅ CustomItemFactory.java              # 物品工厂
✅ ModelCommand.java                   # 命令系统
✅ config.yml                          # 配置文件
✅ items.yml                           # 物品定义
✅ plugin.yml                          # 插件元数据
✅ build.gradle                        # Gradle配置
✅ README.md                           # 使用说明
```

## 联系支持

如遇到构建问题，请提供：
1. 错误日志截图
2. JDK版本确认
3. Gradle版本确认

---

**当前状态**: 所有源代码已创建完成，等待构建JAR文件。