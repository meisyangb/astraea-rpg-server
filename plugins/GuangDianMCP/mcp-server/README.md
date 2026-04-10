# GuangDianMCP 配置示例

## 架构说明

```
┌─────────────────────────────────────────────────────────┐
│                    Trae / Claude Desktop                │
│                        (MCP Client)                     │
└─────────────────────┬───────────────────────────────────┘
                      │ stdio (JSON-RPC)
                      ▼
┌─────────────────────────────────────────────────────────┐
│              guangdian-mcp (Node.js)                   │
│                  (MCP 代理服务器)                        │
└─────────────────────┬───────────────────────────────────┘
                      │ HTTP API
                      ▼
┌─────────────────────────────────────────────────────────┐
│          GuangDianMCP (Bukkit Plugin)                  │
│              (Minecraft 服务器插件)                      │
│                   端口: 8080                            │
└─────────────────────────────────────────────────────────┘
```

## 使用步骤

### 1. 部署Bukkit插件

将 `GuangDianMCP-1.0.0.jar` 放入服务器 `plugins` 目录

修改 `plugins/GuangDianMCP/config.yml`:
```yaml
server:
  port: 8080
  host: "0.0.0.0"
  enabled: true

security:
  tokens:
    - "your-secure-token-change-this"
```

重启服务器

### 2. 安装Node.js依赖

```bash
cd plugins/GuangDianMCP/mcp-server
npm install
npm run build
```

### 3. 配置Trae MCP

找到Trae的MCP配置文件，添加：

**Windows:**
```json
{
  "mcpServers": {
    "minecraft-server": {
      "command": "cmd",
      "args": ["E:\\原创RPG服务端\\plugins\\GuangDianMCP\\mcp-server\\dist\\index.js"],
      "env": {
        "MCP_URL": "http://127.0.0.1:8080",
        "MCP_TOKEN": "your-secure-token"
      }
    }
  }
}
```

**Linux/Mac:**
```json
{
  "mcpServers": {
    "minecraft-server": {
      "command": "node",
      "args": ["/path/to/plugins/GuangDianMCP/mcp-server/dist/index.js"],
      "env": {
        "MCP_URL": "http://127.0.0.1:8080",
        "MCP_TOKEN": "your-secure-token"
      }
    }
  }
}
```

### 4. 启动服务器

确保Minecraft服务器正在运行，然后启动Trae。

## 可用工具

| 工具 | 描述 |
|------|------|
| server_info | 获取服务器状态(TPS、内存、玩家数) |
| player_management | 玩家管理(列表、踢出、封禁、信息) |
| execute_command | 执行控制台命令 |
| world_management | 世界管理(加载、卸载、创建) |
| plugin_management | 插件管理(启用、禁用、重载) |
| read_config | 读取配置文件 |
| read_logs | 读取服务器日志 |
| whitelist | 白名单管理 |
| ban | 封禁管理 |
| teleport | 玩家传送 |
| item | 物品管理 |
| entity | 实体管理 |

## 示例对话

> **You**: 查看服务器状态
> 
> **Trae**: (调用 server_info 工具)
> 
> 返回:
> ```json
> {
>   "serverName": "GuangDian RPG",
>   "version": "1.21.4",
>   "onlinePlayers": 5,
>   "maxPlayers": 100,
>   "tps1m": 20.0,
>   "memory": {"usedMB": 2048, "maxMB": 4096}
> }
> ```

> **You**: 列出所有在线玩家
> 
> **Trae**: (调用 player_management 工具，action: "list")
> 
> 返回玩家列表

> **You**: 踢出玩家 TestPlayer
> 
> **Trae**: (调用 player_management 工具，action: "kick", player: "TestPlayer", reason: "测试")
> 
> 返回: "已踢出玩家: TestPlayer"
