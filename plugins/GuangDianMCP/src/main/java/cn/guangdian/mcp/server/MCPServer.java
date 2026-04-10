package cn.guangdian.mcp.server;

import cn.guangdian.mcp.GuangDianMCP;
import cn.guangdian.mcp.config.MCPConfig;
import cn.guangdian.mcp.protocol.MCPRequestHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class MCPServer {
    
    private final GuangDianMCP plugin;
    private HttpServer server;
    private volatile boolean running = false;
    private final MCPRequestHandler requestHandler;
    private SSEHttpHandler sseHandler;
    
    public MCPServer(GuangDianMCP plugin) {
        this.plugin = plugin;
        this.requestHandler = new MCPRequestHandler(plugin);
    }
    
    public void start() throws IOException {
        MCPConfig config = plugin.getMCPConfig();
        
        server = HttpServer.create(
            new InetSocketAddress(config.getHost(), config.getPort()), 
            0
        );
        
        this.sseHandler = new SSEHttpHandler(plugin);
        
        server.createContext("/mcp", new MCPHttpHandler());
        server.createContext("/sse", sseHandler);
        server.createContext("/health", new HealthCheckHandler());
        
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        running = true;
        
        plugin.getLogger().info("MCP HTTP服务器已启动");
    }
    
    public SSEHttpHandler getSSEHandler() {
        return sseHandler;
    }
    
    public void stop() {
        if (server != null) {
            server.stop(0);
            running = false;
            plugin.getLogger().info("MCP HTTP服务器已停止");
        }
    }
    
    public boolean isRunning() {
        return running;
    }
    
    private class MCPHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
            MCPConfig config = plugin.getMCPConfig();
            
            if (!config.isIpAllowed(clientIp)) {
                sendResponse(exchange, 403, "{\"error\": \"IP not allowed\"}");
                return;
            }
            
            if (!exchange.getRequestMethod().equals("POST")) {
                sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
                return;
            }
            
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
            
            if (token == null) {
                token = exchange.getRequestHeaders().getFirst("X-MCP-Token");
            }
            
            if (token == null || !config.validateToken(token)) {
                sendResponse(exchange, 401, "{\"error\": \"Unauthorized\"}");
                return;
            }
            
            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            
            if (config.isLogRequests()) {
                plugin.getLogger().info("MCP请求来自 " + clientIp + ": " + body.substring(0, Math.min(body.length(), 200)));
            }
            
            String response = requestHandler.handleRequest(body);
            
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            sendResponse(exchange, 200, response);
        }
        
        private void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }
    
    private class HealthCheckHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "{\"status\": \"ok\", \"server\": \"GuangDianMCP\"}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }
}
