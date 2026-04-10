---
name: "minecraft-client-mod"
description: "Minecraft Fabric 客户端模组开发专家。专注于自定义UI界面、登录界面、HUD显示、图形渲染。当用户请求：开发客户端模组、自定义界面、登录界面、GUI开发时触发。"
---

# Minecraft Fabric 客户端模组开发规范

> Astraea RPG 客户端模组开发指南

---

## 环境基线

| 项目 | 值 |
|------|---|
| 模组加载器 | Fabric |
| Minecraft 版本 | 1.21.4 |
| JDK | JDK 21 |
| 构建工具 | Gradle 9.4.0 |
| 客户端目录 | `e:\原创RPG服务端\客户端` |
| 现有模组 | `modern-healthbar-mod` |

---

## 项目结构模板

```
客户端/{mod-name}/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/{mod-id}/
│       │       ├── {ModName}.java           # 主类
│       │       ├── client/
│       │       │   ├── {ModName}Client.java # 客户端入口
│       │       │   ├── gui/                 # GUI界面
│       │       │   ├── render/              # 渲染器
│       │       │   └── handler/             # 事件处理器
│       │       ├── network/                 # 网络通信
│       │       └── config/                  # 配置
│       └── resources/
│           ├── fabric.mod.json              # 模组元数据
│           ├── assets/{mod-id}/
│           │   ├── textures/                # 材质
│           │   ├── lang/                    # 语言文件
│           │   └── sounds/                  # 音效
│           └── {mod-id}.mixins.json         # Mixin配置
├── build.gradle
├── gradle.properties
└── settings.gradle
```

---

## build.gradle 模板

```gradle
plugins {
    id 'fabric-loom' version '1.9-SNAPSHOT'
    id 'maven-publish'
}

version = project.mod_version
group = project.maven_group

base {
    archivesName = project.archives_base_name
}

repositories {
    maven { url "https://maven.shedaniel.me/" }
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
    
    modApi("me.shedaniel.cloth:cloth-config-fabric:15.0.130") {
        exclude(group: "net.fabricmc.fabric-api")
    }
}

def targetJavaVersion = 21
tasks.withType(JavaCompile).configureEach {
    it.options.encoding = "UTF-8"
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible()) {
        it.options.release.set(targetJavaVersion)
    }
}

java {
    withSourcesJar()
}

loom {
    runs {
        client {
            client()
            setConfigName("Fabric Client")
        }
    }
}
```

---

## gradle.properties 模板

```properties
org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=true
org.gradle.java.home=C:\\Program Files\\Java\\jdk21

minecraft_version=1.21.4
yarn_mappings=1.21.4+build.8
loader_version=0.16.9
fabric_version=0.110.5+1.21.4

mod_version=1.0.0
maven_group=com.{mod-id}.mod
archives_base_name={mod-name}
```

---

## fabric.mod.json 模板

```json
{
  "schemaVersion": 1,
  "id": "{mod-id}",
  "version": "${version}",
  "name": "{Mod Name}",
  "description": "Description of the mod",
  "authors": [],
  "contact": {},
  "license": "MIT",
  "entrypoints": {
    "main": [
      "com.{mod-id}.mod.{ModName}"
    ],
    "client": [
      "com.{mod-id}.mod.client.{ModName}Client"
    ]
  },
  "mixins": [
    "{mod-id}.mixins.json"
  ],
  "depends": {
    "fabricloader": ">=0.16.0",
    "minecraft": "~1.21.4",
    "java": ">=21",
    "fabric-api": "*"
  }
}
```

---

## 主类模板

```java
package com.{mod-id}.mod;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class {ModName} implements ModInitializer {
    public static final String MOD_ID = "{mod-id}";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("{} initialized", MOD_ID);
    }
}
```

---

## 客户端入口模板

```java
package com.{mod-id}.mod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class {ModName}Client implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        // 注册事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 每帧逻辑
        });
    }
}
```

---

## 自定义 Screen 界面模板

```java
package com.{mod-id}.mod.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CustomScreen extends Screen {
    
    private static final Identifier BACKGROUND_TEXTURE = 
        Identifier.of("{mod-id}", "textures/gui/background.png");
    
    private TextFieldWidget inputField;
    
    public CustomScreen() {
        super(Text.of("Custom Screen"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        this.inputField = new TextFieldWidget(
            this.textRenderer,
            centerX - 100, centerY - 10,
            200, 20,
            Text.of("Input")
        );
        this.addSelectableChild(this.inputField);
        
        this.addDrawableChild(ButtonWidget.builder(
            Text.of("Confirm"),
            button -> onConfirm()
        ).dimensions(centerX - 50, centerY + 40, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        
        context.drawTexture(
            BACKGROUND_TEXTURE,
            0, 0,
            0, 0,
            this.width, this.height,
            this.width, this.height
        );
        
        context.drawCenteredTextWithShadow(
            this.textRenderer,
            "Welcome to Astraea RPG",
            this.width / 2,
            this.height / 3,
            0xFFD700
        );
        
        this.inputField.render(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
    
    private void onConfirm() {
        String input = this.inputField.getText();
        // 处理输入
        this.client.setScreen(null);
    }
}
```

---

## 网络通信模板（与服务端插件通信）

### 客户端发送

```java
public class ClientNetworkHandler {
    
    private static final Identifier CHANNEL = Identifier.of("guangdian", "auth");
    
    public static void sendPassword(String password) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(password);
            
            ClientPlayNetworking.send(CHANNEL, buf);
        }
    }
}
```

### 客户端接收

```java
public class ClientNetworkHandler {
    
    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(
            Identifier.of("guangdian", "auth_response"),
            (client, handler, buf, responseSender) -> {
                boolean success = buf.readBoolean();
                client.execute(() -> {
                    if (success) {
                        // 登录成功
                    } else {
                        // 登录失败
                    }
                });
            }
        );
    }
}
```

---

## 构建命令

```powershell
cd e:\原创RPG服务端\客户端\{mod-name}
.\gradlew.bat build
```

输出位置：`build/libs/{mod-name}-1.0.0.jar`

---

## 资源文件规范

### 材质位置
```
src/main/resources/assets/{mod-id}/textures/
├── gui/
│   ├── background.png    # 登录背景 (推荐 1920x1080)
│   └── button.png        # 按钮材质
└── icon.png              # 模组图标
```

### 语言文件
```
src/main/resources/assets/{mod-id}/lang/
├── en_us.json
└── zh_cn.json
```

```json
{
  "mod-id.screen.login": "登录",
  "mod-id.screen.password": "密码",
  "mod-id.screen.welcome": "欢迎来到阿斯特瑞亚"
}
```

---

## 注意事项

1. **不要在主线程执行耗时操作**
2. **网络通信必须异步处理**
3. **材质文件使用 PNG 格式，支持透明**
4. **所有文本使用语言文件，支持国际化**
5. **Mixin 用于修改原版行为，谨慎使用**

---

*最后更新: 2026-04-11*
