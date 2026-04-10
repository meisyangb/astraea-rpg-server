package cn.guangdian.mcp;

import cn.guangdian.mcp.server.MCPServer;
import cn.guangdian.mcp.server.EventPusher;
import cn.guangdian.mcp.scheduler.SchedulerManager;
import cn.guangdian.mcp.command.MCPCommand;
import cn.guangdian.mcp.config.MCPConfig;
import cn.guangdian.mcp.tools.ToolRegistry;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

public class GuangDianMCP extends JavaPlugin {
    
    private static GuangDianMCP instance;
    private MCPConfig config;
    private MCPServer mcpServer;
    private ToolRegistry toolRegistry;
    private EventPusher eventPusher;
    private SchedulerManager scheduler;
    
    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        this.config = new MCPConfig(this);
        this.config.load();
        
        this.toolRegistry = new ToolRegistry(this);
        this.toolRegistry.registerDefaultTools();
        
        this.mcpServer = new MCPServer(this);
        
        if (config.isServerEnabled()) {
            startMCPServer();
            this.eventPusher = new EventPusher(this, mcpServer.getSSEHandler());
            this.eventPusher.register();
        } else {
            this.eventPusher = new EventPusher(this, null);
            this.eventPusher.register();
        }
        
        this.scheduler = new SchedulerManager(this);
        loadDefaultTasks();
        this.scheduler.start();
        
        startTPSMonitor();
        
        getCommand("guangdianmcp").setExecutor(new MCPCommand(this));
        
        getLogger().info("GuangDianMCP 已启用!");
        getLogger().info("MCP服务器状态: " + (mcpServer.isRunning() ? "运行中" : "已停止"));
        if (mcpServer.isRunning()) {
            getLogger().info("MCP服务地址: http://" + config.getHost() + ":" + config.getPort());
        }
    }
    
    @Override
    public void onDisable() {
        if (scheduler != null) {
            scheduler.stop();
        }
        
        if (mcpServer != null && mcpServer.isRunning()) {
            stopMCPServer();
        }
        
        getLogger().info("GuangDianMCP 已禁用!");
    }
    
    private void loadDefaultTasks() {
        // 每小时自动保存
        SchedulerManager.ScheduledTask saveTask = new SchedulerManager.ScheduledTask("auto_save", "save_all");
        saveTask.setIntervalSeconds(3600);
        scheduler.addTask(saveTask);
        
        // 每30分钟清理掉落物
        SchedulerManager.ScheduledTask clearLagTask = new SchedulerManager.ScheduledTask("auto_clear_lag", "clear_lag");
        clearLagTask.setIntervalSeconds(1800);
        scheduler.addTask(clearLagTask);
    }
    
    public void startMCPServer() {
        if (mcpServer.isRunning()) {
            getLogger().warning("MCP服务器已在运行中");
            return;
        }
        
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                mcpServer.start();
                getLogger().info("MCP服务器已启动在端口 " + config.getPort());
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "启动MCP服务器失败", e);
            }
        });
    }
    
    public void stopMCPServer() {
        if (!mcpServer.isRunning()) {
            getLogger().warning("MCP服务器未运行");
            return;
        }
        
        try {
            mcpServer.stop();
            getLogger().info("MCP服务器已停止");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "停止MCP服务器失败", e);
        }
    }
    
    public void reloadConfiguration() {
        config.load();
        getLogger().info("配置已重新加载");
    }
    
    private void startTPSMonitor() {
        getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            private double lastTPS = 20.0;
            
            @Override
            public void run() {
                double[] tpsValues = getServer().getTPS();
                double currentTPS = tpsValues[0];
                
                if (currentTPS < 18.0 && lastTPS >= 18.0) {
                    if (eventPusher != null) {
                        eventPusher.pushTPSWarning(currentTPS);
                    }
                }
                
                lastTPS = currentTPS;
                
                Runtime runtime = Runtime.getRuntime();
                long usedMB = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
                long maxMB = runtime.maxMemory() / 1024 / 1024;
                
                if (usedMB > maxMB * 0.85) {
                    if (eventPusher != null) {
                        eventPusher.pushMemoryWarning(usedMB, maxMB);
                    }
                }
            }
        }, 20L * 30, 20L * 30);
    }
    
    public static GuangDianMCP getInstance() {
        return instance;
    }
    
    public MCPConfig getMCPConfig() {
        return config;
    }
    
    public MCPServer getMCPServer() {
        return mcpServer;
    }
    
    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }
    
    public EventPusher getEventPusher() {
        return eventPusher;
    }
    
    public SchedulerManager getScheduler() {
        return scheduler;
    }
}
