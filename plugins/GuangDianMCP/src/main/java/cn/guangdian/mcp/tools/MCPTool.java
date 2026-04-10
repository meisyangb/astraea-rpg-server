package cn.guangdian.mcp.tools;

import com.google.gson.JsonObject;

import java.util.Map;

public interface MCPTool {
    
    String getName();
    
    String getDescription();
    
    JsonObject getInputSchema();
    
    ToolResult execute(Map<String, Object> arguments);
    
    class ToolResult {
        private final String content;
        private final boolean error;
        
        public ToolResult(String content) {
            this(content, false);
        }
        
        public ToolResult(String content, boolean error) {
            this.content = content;
            this.error = error;
        }
        
        public String getContent() {
            return content;
        }
        
        public boolean isError() {
            return error;
        }
        
        public static ToolResult success(String content) {
            return new ToolResult(content, false);
        }
        
        public static ToolResult error(String message) {
            return new ToolResult(message, true);
        }
    }
}
