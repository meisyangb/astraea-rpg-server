package cn.guangdian.custommodels.listener;

import cn.guangdian.custommodels.config.CustomModelsConfig;
import cn.guangdian.custommodels.resourcepack.ResourcePackGenerator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * 资源包发送监听器
 * 自动发送资源包给连接的玩家
 *
 * 修复：添加内置HTTP服务器，当未配置外部URL时也能实际发送资源包给玩家
 */
public class ResourcePackSender implements Listener {

    private final JavaPlugin plugin;
    private final CustomModelsConfig config;
    private final ResourcePackGenerator resourcePackGenerator;
    private File resourcePackFile;
    private byte[] packHash;
    private String packHashHex;
    private SimpleHttpServer httpServer;
    private int serverPort;

    public ResourcePackSender(JavaPlugin plugin, CustomModelsConfig config, ResourcePackGenerator resourcePackGenerator) {
        this.plugin = plugin;
        this.config = config;
        this.resourcePackGenerator = resourcePackGenerator;
        this.resourcePackFile = null;
        this.packHash = null;
        this.packHashHex = "";
        this.serverPort = 0;  // 0表示自动选择端口
    }

    /**
     * 设置资源包文件
     */
    public void setResourcePackFile(File file) {
        this.resourcePackFile = file;
        if (file != null && file.exists()) {
            // 计算资源包的SHA1哈希值（用于验证）— 使用 try-with-resources
            try {
                this.packHash = calculateSHA1(file);
                this.packHashHex = bytesToHex(packHash);
                plugin.getLogger().info("资源包哈希值已计算: " + packHashHex);

                // 如果没有外部URL，启动内置HTTP服务器
                String downloadUrl = config.getDownloadUrl();
                if (downloadUrl == null || downloadUrl.isEmpty()) {
                    startHttpServer();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("无法计算资源包哈希值: " + e.getMessage());
            }
        }
    }

    /**
     * 启动内置HTTP服务器来托管资源包
     */
    private void startHttpServer() {
        if (httpServer != null) {
            return;  // 已启动
        }

        try {
            // 从配置读取端口，默认8964
            serverPort = plugin.getConfig().getInt("resource_pack.server_port", 8964);

            httpServer = new SimpleHttpServer(resourcePackFile, serverPort);
            httpServer.start();

            plugin.getLogger().info("内置HTTP服务器已启动，端口: " + httpServer.getBoundPort());
            plugin.getLogger().info("资源包下载地址: http://localhost:" + httpServer.getBoundPort() + "/resourcepack.zip");
        } catch (Exception e) {
            plugin.getLogger().severe("内置HTTP服务器启动失败: " + e.getMessage());
            plugin.getLogger().severe("请配置外部下载URL: resource_pack.download_url");
        }
    }

    /**
     * 停止HTTP服务器
     */
    public void stopHttpServer() {
        if (httpServer != null) {
            httpServer.stop();
            httpServer = null;
            plugin.getLogger().info("内置HTTP服务器已停止");
        }
    }

    /**
     * 监听玩家加入事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!config.isAutoSendToPlayer()) {
            return;
        }

        Player player = event.getPlayer();

        // 延迟1秒发送资源包，避免玩家刚加入时接收失败
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            sendResourcePack(player);
        }, 20L); // 20 ticks = 1秒
    }

    /**
     * 监听资源包加载状态
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        PlayerResourcePackStatusEvent.Status status = event.getStatus();
        Player player = event.getPlayer();

        switch (status) {
            case SUCCESSFULLY_LOADED:
                plugin.getLogger().info("玩家 " + player.getName() + " 资源包加载成功");
                break;
            case DECLINED:
                plugin.getLogger().warning("玩家 " + player.getName() + " 拒绝了资源包");
                player.sendMessage("§e[提示] §f拒绝资源包将无法看到自定义武器外观");
                break;
            case FAILED_DOWNLOAD:
                plugin.getLogger().warning("玩家 " + player.getName() + " 资源包下载失败");
                player.sendMessage("§c[警告] §f资源包下载失败，请重新加入服务器尝试");
                break;
            case FAILED_RELOAD:
                plugin.getLogger().warning("玩家 " + player.getName() + " 资源包加载失败");
                break;
        }
    }

    /**
     * 发送资源包给玩家
     *
     * 修复：实际调用API发送资源包，不再只打印日志
     */
    private void sendResourcePack(Player player) {
        String downloadUrl = config.getDownloadUrl();

        if (downloadUrl != null && !downloadUrl.isEmpty()) {
            // 使用配置的外部URL
            try {
                player.setResourcePack(downloadUrl, packHash);
                plugin.getLogger().info("向玩家 " + player.getName() + " 发送资源包 (外部URL)");
            } catch (Exception e) {
                plugin.getLogger().warning("发送资源包失败: " + e.getMessage());
            }
        } else if (httpServer != null && httpServer.isRunning()) {
            // 使用内置HTTP服务器
            // 获取服务器IP地址，而非localhost，确保远程客户端也能访问
            String serverIp = getServerIpAddress();
            String localUrl = "http://" + serverIp + ":" + httpServer.getBoundPort() + "/resourcepack.zip";

            try {
                player.setResourcePack(localUrl, packHash);
                plugin.getLogger().info("向玩家 " + player.getName() + " 发送资源包 (内置HTTP: " + localUrl + ")");
            } catch (Exception e) {
                plugin.getLogger().warning("发送资源包失败: " + e.getMessage());
                player.sendMessage("§e[提示] §f资源包发送失败，请手动下载: " + localUrl);
            }
        } else if (resourcePackFile != null && resourcePackFile.exists()) {
            // 没有URL也没有HTTP服务器，提示管理员
            plugin.getLogger().warning("无法发送资源包给玩家 " + player.getName() + ": 未配置下载URL且HTTP服务器未启动");
            plugin.getLogger().warning("请在 config.yml 中配置 resource_pack.download_url");
            plugin.getLogger().warning("或在 server.properties 中配置:");
            plugin.getLogger().warning("  resource-pack=http://your-url/GuangDian_CustomModels.zip");
            plugin.getLogger().warning("  resource-pack-sha1=" + packHashHex);
            player.sendMessage("§e[提示] §f服务器资源包已生成，请联系管理员配置下载URL");
        } else {
            plugin.getLogger().warning("无法发送资源包给玩家 " + player.getName() + ": 资源包文件不存在");
        }
    }

    /**
     * 获取服务器IP地址
     * 尝试多种方式获取可被客户端访问的IP地址
     */
    private String getServerIpAddress() {
        // 1. 优先使用Bukkit服务器绑定的IP
        String serverIp = plugin.getServer().getIp();
        if (serverIp != null && !serverIp.isEmpty() && !"0.0.0.0".equals(serverIp)) {
            return serverIp;
        }

        // 2. 尝试通过本地网络接口获取非回环IP
        try {
            var interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                var iface = interfaces.nextElement();
                var addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    var addr = addresses.nextElement();
                    if (addr.isSiteLocalAddress() && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 3. 如果玩家已在线，使用玩家连接的地址推断
        var onlinePlayers = plugin.getServer().getOnlinePlayers();
        if (!onlinePlayers.isEmpty()) {
            var firstPlayer = onlinePlayers.iterator().next();
            String playerHost = firstPlayer.getAddress().getAddress().getHostAddress();
            // 如果玩家来自局域网，服务器IP应该是同一网段
            if (!playerHost.equals("127.0.0.1")) {
                return playerHost; // 同网段，可能就是服务器IP
            }
        }

        // 4. 兜底：使用localhost（仅本地连接可用）
        return "127.0.0.1";
    }

    /**
     * 计算文件的SHA1哈希值 — 使用 try-with-resources 防止资源泄露
     */
    private byte[] calculateSHA1(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }
        return md.digest();
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 内置简易HTTP服务器
     * 用于在未配置外部URL时托管资源包ZIP文件
     */
    private static class SimpleHttpServer {
        private final File resourcePackFile;
        private final int configuredPort;
        private com.sun.net.httpserver.HttpServer server;
        private volatile boolean running = false;
        private int boundPort;

        public SimpleHttpServer(File resourcePackFile, int port) {
            this.resourcePackFile = resourcePackFile;
            this.configuredPort = port;
        }

        public void start() throws IOException {
            server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("0.0.0.0", configuredPort), 0);
            server.setExecutor(Executors.newFixedThreadPool(2));

            // 注册资源包下载路径
            server.createContext("/resourcepack.zip", exchange -> {
                try {
                    if (!"GET".equals(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(405, -1);
                        exchange.close();
                        return;
                    }

                    if (resourcePackFile == null || !resourcePackFile.exists()) {
                        exchange.sendResponseHeaders(404, -1);
                        exchange.close();
                        return;
                    }

                    byte[] fileBytes = Files.readAllBytes(resourcePackFile.toPath());
                    exchange.getResponseHeaders().set("Content-Type", "application/zip");
                    exchange.getResponseHeaders().set("Content-Length", String.valueOf(fileBytes.length));
                    exchange.sendResponseHeaders(200, fileBytes.length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(fileBytes);
                    }
                } catch (Exception e) {
                    try {
                        exchange.sendResponseHeaders(500, -1);
                        exchange.close();
                    } catch (Exception ignored) {
                    }
                }
            });

            server.start();
            boundPort = server.getAddress().getPort();
            running = true;
        }

        public void stop() {
            if (server != null) {
                server.stop(0);
                running = false;
            }
        }

        public boolean isRunning() {
            return running;
        }

        public int getBoundPort() {
            return boundPort;
        }
    }
}
