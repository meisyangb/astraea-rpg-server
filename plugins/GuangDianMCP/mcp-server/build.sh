#!/bin/bash

# GuangDianMCP 构建和运行脚本

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo "=== GuangDianMCP Node.js Server ==="

# 检查Node.js
if ! command -v node &> /dev/null; then
    echo "错误: 未找到Node.js，请先安装Node.js"
    exit 1
fi

# 安装依赖
if [ ! -d "node_modules" ]; then
    echo "安装依赖..."
    npm install
fi

# 构建
echo "构建中..."
npm run build

if [ $? -eq 0 ]; then
    echo "构建成功!"
    echo ""
    echo "要配置Trae MCP，请添加以下配置:"
    echo ""
    echo 'Windows:'
    echo "  {\"
    echo "    \"mcpServers\": {\"
    echo "      \"minecraft-server\": {\"
    echo "        \"command\": \"node\",\"
    echo "        \"args\": [\"$SCRIPT_DIR/dist/index.js\"]\"
    echo "      }\"
    echo "    }\"
    echo "  }"
    echo ""
    echo "确保Minecraft服务器已启动并加载GuangDianMCP插件!"
else
    echo "构建失败!"
    exit 1
fi
