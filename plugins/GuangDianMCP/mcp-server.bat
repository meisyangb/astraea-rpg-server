@echo off
setlocal enabledelayedexpansion

set "TOKEN="
set "MCP_URL=http://127.0.0.1:8080"

if "%1"=="init" goto init
if "%1"=="tools/list" goto tools_list
if "%1"=="tools/call" goto tools_call
if "%1"=="resources/list" goto resources_list
if "%1"=="resources/read" goto resources_read
if "%1"=="prompts/list" goto prompts_list
if "%1"=="prompts/get" goto prompts_get
if "%1"=="start" goto start_server

echo Usage: mcp-server.bat [command]
echo Commands:
echo   init           - Initialize MCP connection
echo   tools/list     - List available tools
echo   tools/call     - Call a tool
echo   resources/list - List resources
echo   resources/read - Read a resource
echo   prompts/list   - List prompts
echo   prompts/get    - Get a prompt
echo   start          - Start MCP server
exit /b 0

:start_server
echo Starting MCP server...
echo Please ensure GuangDianMCP plugin is loaded in your Minecraft server
echo.
echo To configure in Claude Desktop / Trae, add this to your MCP config:
echo {
echo   "mcpServers": {
echo     "minecraft-server": {
echo       "command": "cmd", 
echo       "args": ["/c", "path\\to\\mcp-server.bat"]
echo     }
echo   }
echo }
exit /b 0

:init
echo {"protocolVersion":"2024-11-05","capabilities":{},"serverInfo":{"name":"GuangDianMCP","version":"1.0.0"}}
exit /b 0

:tools_list
curl -s -X POST "%MCP_URL%/mcp" -H "Content-Type: application/json" -H "Authorization: Bearer %TOKEN%" -d "{\"jsonrpc\":\"2.0\",\"method\":\"tools/list\",\"params\":{},\"id\":1}"
exit /b 0

:tools_call
set "TOOL_NAME=%2"
set "ARGS=%3"
curl -s -X POST "%MCP_URL%/mcp" -H "Content-Type: application/json" -H "Authorization: Bearer %TOKEN%" -d "{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\",\"params\":{\"name\":\"%TOOL_NAME%\",\"arguments\":%ARGS%},\"id\":1}"
exit /b 0

:resources_list
curl -s -X POST "%MCP_URL%/mcp" -H "Content-Type: application/json" -H "Authorization: Bearer %TOKEN%" -d "{\"jsonrpc\":\"2.0\",\"method\":\"resources/list\",\"params\":{},\"id\":1}"
exit /b 0

:resources_read
set "URI=%2"
curl -s -X POST "%MCP_URL%/mcp" -H "Content-Type: application/json" -H "Authorization: Bearer %TOKEN%" -d "{\"jsonrpc\":\"2.0\",\"method\":\"resources/read\",\"params\":{\"uri\":\"%URI%\"},\"id\":1}"
exit /b 0

:prompts_list
curl -s -X POST "%MCP_URL%/mcp" -H "Content-Type: application/json" -H "Authorization: Bearer %TOKEN%" -d "{\"jsonrpc\":\"2.0\",\"method\":\"prompts/list\",\"params\":{},\"id\":1}"
exit /b 0

:prompts_get
set "NAME=%2"
curl -s -X POST "%MCP_URL%/mcp" -H "Content-Type: application/json" -H "Authorization: Bearer %TOKEN%" -d "{\"jsonrpc\":\"2.0\",\"method\":\"prompts/get\",\"params\":{\"name\":\"%NAME%\"},\"id\":1}"
exit /b 0
