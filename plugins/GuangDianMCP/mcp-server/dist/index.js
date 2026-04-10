import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { CallToolRequestSchema, ListToolsRequestSchema, ListResourcesRequestSchema, ReadResourceRequestSchema, ListPromptsRequestSchema, GetPromptRequestSchema, } from '@modelcontextprotocol/sdk/types.js';
import axios from 'axios';
const MCP_URL = process.env.MCP_URL || 'http://127.0.0.1:8080';
const TOKEN = process.env.MCP_TOKEN || '';
const server = new Server({
    name: 'guangdian-mcp',
    version: '1.0.0',
}, {
    capabilities: {
        tools: {},
        resources: {},
        prompts: {},
    },
});
async function mcpRequest(method, params = {}) {
    try {
        const response = await axios.post(`${MCP_URL}/mcp`, {
            jsonrpc: '2.0',
            method,
            params,
            id: Date.now(),
        }, {
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${TOKEN}`,
            },
            timeout: 30000,
        });
        return response.data;
    }
    catch (error) {
        throw new Error(`MCP请求失败: ${error.message}`);
    }
}
server.setRequestHandler(ListToolsRequestSchema, async () => {
    const response = await mcpRequest('tools/list');
    return response.result || { tools: [] };
});
server.setRequestHandler(CallToolRequestSchema, async (request) => {
    const { name, arguments: args } = request.params;
    try {
        const response = await mcpRequest('tools/call', {
            name,
            arguments: args || {},
        });
        if (response.error) {
            return {
                content: [
                    {
                        type: 'text',
                        text: `错误: ${response.error.message || JSON.stringify(response.error)}`,
                    },
                ],
                isError: true,
            };
        }
        return response.result || { content: [], isError: false };
    }
    catch (error) {
        return {
            content: [
                {
                    type: 'text',
                    text: `执行失败: ${error.message}`,
                },
            ],
            isError: true,
        };
    }
});
server.setRequestHandler(ListResourcesRequestSchema, async () => {
    const response = await mcpRequest('resources/list');
    return response.result || { resources: [] };
});
server.setRequestHandler(ReadResourceRequestSchema, async (request) => {
    const { uri } = request.params;
    const response = await mcpRequest('resources/read', { uri });
    return response.result || { contents: [] };
});
server.setRequestHandler(ListPromptsRequestSchema, async () => {
    const response = await mcpRequest('prompts/list');
    return response.result || { prompts: [] };
});
server.setRequestHandler(GetPromptRequestSchema, async (request) => {
    const { name, arguments: args } = request.params;
    const response = await mcpRequest('prompts/get', { name, arguments: args || {} });
    return response.result || { messages: [] };
});
async function main() {
    console.error('GuangDian MCP Server 启动中...');
    console.error(`连接地址: ${MCP_URL}`);
    const transport = new StdioServerTransport();
    await server.connect(transport);
    console.error('GuangDian MCP Server 已连接');
}
main().catch((error) => {
    console.error('启动失败:', error);
    process.exit(1);
});
