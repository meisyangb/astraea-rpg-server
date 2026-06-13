package cn.guangdian.mcp.server;

import cn.guangdian.mcp.GuangDianMCP;
import cn.guangdian.mcp.config.MCPConfig;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SSEHttpHandler implements HttpHandler {
    
    private final GuangDianMCP plugin;
    private final Map<String, PrintWriter> clients = new ConcurrentHashMap<>();
    
    public SSEHttpHandler(GuangDianMCP plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
        MCPConfig config = plugin.getMCPConfig();
        
        if (!config.isIpAllowed(clientIp)) {
            sendError(exchange, 403, "IP not allowed");
            return;
        }
        
        String token = exchange.getRequestHeaders().getFirst("X-MCP-Token");
        if (token == null) {
            token = getQueryParam(exchange.getRequestURI().getQuery(), "token");
        }
        
        if (token == null || !config.validateToken(token)) {
            sendError(exchange, 401, "Unauthorized");
            return;
        }
        
        if (!exchange.getRequestMethod().equals("GET")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, 0);
        
        OutputStream os = exchange.getResponseBody();
        PrintWriter writer = new PrintWriter(os, true, StandardCharsets.UTF_8);
        
        String clientId = UUID.randomUUID().toString();
        clients.put(clientId, writer);
        
        plugin.getLogger().info("SSE客户端连接: " + clientId + " from " + clientIp);
        
        writer.println("event: connected");
        writer.println("data: {\"clientId\": \"" + clientId + "\"}");
        writer.println();
        
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(30000);
                writer.println("event: ping");
                writer.println("data: {}");
                writer.println();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            clients.remove(clientId);
            writer.close();
            plugin.getLogger().info("SSE客户端断开: " + clientId);
        }
    }
    
    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        String response = "{\"error\": \"" + message + "\"}";
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
    
    private String getQueryParam(String query, String param) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(param)) {
                return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
    
    public void broadcast(String event, String data) {
        for (PrintWriter writer : clients.values()) {
            writer.println("event: " + event);
            writer.println("data: " + data);
            writer.println();
        }
    }
    
    public void sendToClient(String clientId, String event, String data) {
        PrintWriter writer = clients.get(clientId);
        if (writer != null) {
            writer.println("event: " + event);
            writer.println("data: " + data);
            writer.println();
        }
    }
}
