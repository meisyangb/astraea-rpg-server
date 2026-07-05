package cn.guangdian.custommodels;

import cn.guangdian.custommodels.command.ModelCommand;
import cn.guangdian.custommodels.config.CustomModelsConfig;
import cn.guangdian.custommodels.factory.CustomItemFactory;
import cn.guangdian.custommodels.listener.ResourcePackSender;
import cn.guangdian.custommodels.model.ModelGenerator;
import cn.guangdian.custommodels.registry.CustomItemRegistry;
import cn.guangdian.custommodels.resourcepack.ResourcePackGenerator;
import cn.guangdian.custommodels.texture.TextureManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * GuangDianCustomModels 主类
 * 完全独立的自定义武器贴图和模型插件
 * 不依赖任何外部插件
 *
 * @author GuangDian Team
 * @version 1.1.0
 */
public class GuangDianCustomModels extends JavaPlugin {

    private static GuangDianCustomModels instance;

    private CustomModelsConfig configManager;
    private TextureManager textureManager;
    private ModelGenerator modelGenerator;
    private ResourcePackGenerator resourcePackGenerator;
    private CustomItemRegistry itemRegistry;
    private CustomItemFactory itemFactory;
    private ResourcePackSender resourcePackSender;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("========== GuangDianCustomModels 启动 ==========");

        // 1. 初始化配置管理器
        configManager = new CustomModelsConfig(this);
        configManager.loadConfig();
        getLogger().info("配置管理器已初始化");

        // 2. 初始化贴图管理器
        textureManager = new TextureManager(this, configManager);
        getLogger().info("贴图管理器已初始化");

        // 确保贴图目录存在
        File textureDir = new File(configManager.getTextureSourceDirectory());
        if (!textureDir.exists()) {
            textureDir.mkdirs();
            getLogger().info("贴图目录已创建: " + textureDir.getAbsolutePath());
            getLogger().info("请将贴图文件放置到此目录后，使用 /custommodels scan 命令扫描");
        }

        // 3. 初始化模型生成器
        modelGenerator = new ModelGenerator(this, configManager);
        getLogger().info("模型生成器已初始化");

        // 4. 初始化资源包生成器
        resourcePackGenerator = new ResourcePackGenerator(this, configManager);
        getLogger().info("资源包生成器已初始化");

        // 5. 初始化物品注册表
        itemRegistry = new CustomItemRegistry(this, configManager);
        getLogger().info("物品注册表已初始化");

        // 关键修复：将物品注册表传递给资源包生成器
        // 这样资源包中的 overrides 使用注册表已分配的 CustomModelData，
        // 确保物品给予玩家时的 CMD 与资源包 overrides 完全一致
        resourcePackGenerator.setItemRegistry(itemRegistry);
        getLogger().info("资源包生成器已关联物品注册表");

        // 6. 初始化物品工厂
        itemFactory = new CustomItemFactory(this, itemRegistry);
        getLogger().info("物品工厂已初始化");

        // 7. 注册命令
        if (getCommand("custommodels") != null) {
            ModelCommand modelCommand = new ModelCommand(this, textureManager, modelGenerator, resourcePackGenerator, itemRegistry, itemFactory);
            getCommand("custommodels").setExecutor(modelCommand);
            getCommand("custommodels").setTabCompleter(modelCommand);
            getLogger().info("命令系统已注册");
        }

        // 8. 注册资源包发送监听器
        resourcePackSender = new ResourcePackSender(this, configManager, resourcePackGenerator);
        getServer().getPluginManager().registerEvents(resourcePackSender, this);
        getLogger().info("资源包发送监听器已注册");

        // 9. 自动扫描贴图（异步）
        if (configManager.isAutoScanEnabled()) {
            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                textureManager.scanTextures();

                // 扫描完成后在主线程注册物品
                getServer().getScheduler().runTask(this, () -> {
                    itemRegistry.generateItemDefinitionsFromTextures(textureManager);
                    getLogger().info("物品注册完成，共 " + itemRegistry.getItemCount() + " 个");

                    // 10. 自动生成资源包
                    if (configManager.isAutoGenerateResourcePack()) {
                        generateResourcePack();
                    }
                });
            });
        }

        getLogger().info("======================================");
        getLogger().info("GuangDianCustomModels v" + getDescription().getVersion() + " 已启动!");
        getLogger().info("======================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("========== GuangDianCustomModels 关闭 ==========");

        // 停止内置HTTP服务器
        if (resourcePackSender != null) {
            resourcePackSender.stopHttpServer();
            getLogger().info("HTTP服务器已停止");
        }

        // 保存配置
        if (configManager != null) {
            configManager.saveConfig();
            getLogger().info("配置已保存");
        }

        // 清理资源
        if (textureManager != null) {
            textureManager.clearCache();
            getLogger().info("贴图缓存已清理");
        }

        getLogger().info("======================================");
        getLogger().info("GuangDianCustomModels 已关闭");
        getLogger().info("======================================");

        instance = null;
    }

    /**
     * 生成资源包（异步执行，避免阻塞主线程）
     */
    private void generateResourcePack() {
        getLogger().info("开始异步生成资源包...");

        // 使用异步任务生成资源包
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                getLogger().info("异步任务开始...");

                // 生成模型JSON文件
                modelGenerator.generateModels(textureManager);

                // 打包资源包
                File resourcePack = resourcePackGenerator.generatePack(textureManager, modelGenerator);

                if (resourcePack != null && resourcePack.exists()) {
                    getLogger().info("资源包已生成: " + resourcePack.getAbsolutePath());
                    getLogger().info("资源包大小: " + (resourcePack.length() / 1024 / 1024) + " MB");

                    // 设置资源包文件给发送监听器（需要在主线程执行）
                    getServer().getScheduler().runTask(this, () -> {
                        if (resourcePackSender != null) {
                            resourcePackSender.setResourcePackFile(resourcePack);
                            getLogger().info("资源包已准备好发送给玩家");
                        }
                    });
                } else {
                    getLogger().warning("资源包生成失败");
                }
            } catch (Exception e) {
                getLogger().severe("资源包异步生成失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public static GuangDianCustomModels getInstance() {
        return instance;
    }

    public CustomModelsConfig getConfigManager() {
        return configManager;
    }

    public TextureManager getTextureManager() {
        return textureManager;
    }

    public ModelGenerator getModelGenerator() {
        return modelGenerator;
    }

    public ResourcePackGenerator getResourcePackGenerator() {
        return resourcePackGenerator;
    }

    public CustomItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    public CustomItemFactory getItemFactory() {
        return itemFactory;
    }

    public ResourcePackSender getResourcePackSender() {
        return resourcePackSender;
    }
}
