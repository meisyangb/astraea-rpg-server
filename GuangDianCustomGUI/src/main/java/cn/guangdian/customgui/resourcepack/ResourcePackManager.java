package cn.guangdian.customgui.resourcepack;

import cn.guangdian.customgui.GuangDianCustomGUI;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ResourcePackManager {

    private final GuangDianCustomGUI plugin;
    private String resourcePackUrl;
    private String resourcePackHash;
    private File resourcePackFile;

    public ResourcePackManager(GuangDianCustomGUI plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载资源包配置
     */
    public void load() {
        boolean enabled = plugin.getConfig().getBoolean("resource-pack.enabled", false);
        if (!enabled) {
            plugin.getLogger().info("资源包未启用，跳过加载");
            return;
        }

        // 检查本地资源包文件
        String packPath = plugin.getConfig().getString("resource-pack.local-file", "resourcepack.zip");
        resourcePackFile = new File(plugin.getDataFolder(), packPath);

        if (!resourcePackFile.exists()) {
            // 尝试从resourcepack目录打包
            File resourceDir = new File(plugin.getDataFolder(), "resourcepack");
            if (resourceDir.exists()) {
                plugin.getLogger().info("正在从 resourcepack 目录打包资源包...");
                packResourcePack(resourceDir);
            } else {
                plugin.getLogger().warning("资源包文件不存在: " + resourcePackFile.getAbsolutePath());
                return;
            }
        }

        // 计算SHA-1哈希
        try {
            resourcePackHash = calculateSHA1(resourcePackFile);
            plugin.getLogger().info("资源包SHA-1: " + resourcePackHash);
        } catch (Exception e) {
            plugin.getLogger().warning("计算资源包哈希失败: " + e.getMessage());
        }

        // 获取URL配置
        resourcePackUrl = plugin.getConfig().getString("resource-pack.url", "");
        if (resourcePackUrl.isEmpty()) {
            plugin.getLogger().info("未配置资源包URL，将使用本地文件模式");
        } else {
            plugin.getLogger().info("资源包URL: " + resourcePackUrl);
        }

        plugin.getLogger().info("资源包加载完成: " + resourcePackFile.getName());
    }

    /**
     * 打包资源包目录为ZIP
     */
    private void packResourcePack(File sourceDir) {
        try (FileOutputStream fos = new FileOutputStream(resourcePackFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            
            zipDirectory(sourceDir, "", zos);
            plugin.getLogger().info("资源包打包完成: " + resourcePackFile.getAbsolutePath());
        } catch (IOException e) {
            plugin.getLogger().warning("打包资源包失败: " + e.getMessage());
        }
    }

    /**
     * 递归打包目录
     */
    private void zipDirectory(File folder, String parent, ZipOutputStream zos) throws IOException {
        for (File file : folder.listFiles()) {
            String entryName = parent.isEmpty() ? file.getName() : parent + "/" + file.getName();
            if (file.isDirectory()) {
                zipDirectory(file, entryName, zos);
            } else {
                zos.putNextEntry(new ZipEntry(entryName));
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    /**
     * 计算文件SHA-1哈希
     */
    private String calculateSHA1(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                digest.update(buffer, 0, length);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 发送资源包给玩家
     */
    public void sendToPlayer(Player player) {
        if (resourcePackUrl.isEmpty()) {
            return; // 未配置URL，不发送
        }

        if (!plugin.getConfig().getBoolean("resource-pack.send-on-join", true)) {
            return; // 未启用加入时发送
        }

        try {
            if (resourcePackHash != null && !resourcePackHash.isEmpty()) {
                player.setResourcePack(resourcePackUrl, resourcePackHash);
            } else {
                player.setResourcePack(resourcePackUrl);
            }
            plugin.getLogger().info("已向玩家 " + player.getName() + " 发送资源包请求");
        } catch (Exception e) {
            plugin.getLogger().warning("向玩家 " + player.getName() + " 发送资源包失败: " + e.getMessage());
        }
    }

    /**
     * 强制发送资源包给玩家
     */
    public void forceSendToPlayer(Player player, String url) {
        try {
            player.setResourcePack(url);
            plugin.getLogger().info("已强制向玩家 " + player.getName() + " 发送资源包: " + url);
        } catch (Exception e) {
            plugin.getLogger().warning("强制发送资源包失败: " + e.getMessage());
        }
    }

    public String getResourcePackUrl() {
        return resourcePackUrl;
    }

    public String getResourcePackHash() {
        return resourcePackHash;
    }

    public File getResourcePackFile() {
        return resourcePackFile;
    }
}
