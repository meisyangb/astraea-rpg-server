package cn.guangdian.custommodels.resourcepack;

import cn.guangdian.custommodels.config.CustomModelsConfig;
import cn.guangdian.custommodels.model.ModelGenerator;
import cn.guangdian.custommodels.registry.CustomItemRegistry;
import cn.guangdian.custommodels.texture.TextureManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 资源包生成器
 * 生成标准Minecraft资源包ZIP文件
 *
 * 修复：使用 CustomItemRegistry 作为 CMD 分配的唯一来源，
 * 确保注册表和 overrides 中的 CustomModelData 完全一致
 */
public class ResourcePackGenerator {

    private final JavaPlugin plugin;
    private final CustomModelsConfig config;
    private CustomItemRegistry itemRegistry;

    public ResourcePackGenerator(JavaPlugin plugin, CustomModelsConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * 设置物品注册表（必须在生成资源包之前调用）
     */
    public void setItemRegistry(CustomItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    /**
     * 生成资源包
     */
    public File generatePack(TextureManager textureManager, ModelGenerator modelGenerator) {
        String outputDir = config.getOutputDirectory();
        String packName = config.getPackName();
        int packFormat = config.getPackFormat();

        File outputDirectory = new File(outputDir);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        File zipFile = new File(outputDirectory, packName + ".zip");

        plugin.getLogger().info("开始生成资源包: " + zipFile.getAbsolutePath());

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile))) {
            // 1. 添加 pack.mcmeta
            addPackMeta(zipOut, packFormat);

            // 2. 添加贴图文件
            addTextures(zipOut, textureManager);

            // 3. 添加模型文件
            addModels(zipOut, modelGenerator);

            // 4. 添加基础物品的 overrides 模型文件（关键！）
            addBaseItemOverrides(zipOut, textureManager);

            plugin.getLogger().info("资源包生成成功");
            return zipFile;

        } catch (IOException e) {
            plugin.getLogger().severe("资源包生成失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 添加 pack.mcmeta
     */
    private void addPackMeta(ZipOutputStream zipOut, int packFormat) throws IOException {
        String metaJson = "{\n" +
                "  \"pack\": {\n" +
                "    \"pack_format\": " + packFormat + ",\n" +
                "    \"description\": \"GuangDian Custom Models Resource Pack\"\n" +
                "  }\n" +
                "}\n";

        ZipEntry entry = new ZipEntry("pack.mcmeta");
        zipOut.putNextEntry(entry);
        zipOut.write(metaJson.getBytes());
        zipOut.closeEntry();

        plugin.getLogger().info("pack.mcmeta 已添加 (format: " + packFormat + ")");
    }

    /**
     * 添加贴图文件
     */
    private void addTextures(ZipOutputStream zipOut, TextureManager textureManager) throws IOException {
        int count = 0;
        int skippedNonWeapon = 0;

        for (TextureManager.TextureInfo texture : textureManager.getAllTextures().values()) {
            // ★ 跳过非武器类贴图（称号、魂环等）
            String category = texture.getCategory();
            if (category.equals("称号") || category.equals("魂环")) {
                skippedNonWeapon++;
                continue;
            }

            File textureFile = new File(texture.getFilePath());
            if (!textureFile.exists()) {
                continue;
            }

            // ★ 资源包中的路径: assets/guangdian/textures/weapons/净化ID.png
            // 使用净化后的ID（已去除非法字符）和统一的.png扩展名
            String zipPath = "assets/guangdian/textures/weapons/" + texture.getSanitizedFileName();

            ZipEntry entry = new ZipEntry(zipPath);
            zipOut.putNextEntry(entry);

            // 复制贴图文件（从原始文件读取，写入净化路径）
            Files.copy(textureFile.toPath(), zipOut);

            zipOut.closeEntry();

            count++;
            if (count % 500 == 0) {
                plugin.getLogger().info("已添加 " + count + " 个贴图");
            }
        }

        if (skippedNonWeapon > 0) {
            plugin.getLogger().info("跳过非武器贴图 " + skippedNonWeapon + " 个（称号/魂环等）");
        }
        plugin.getLogger().info("武器贴图添加完成，共 " + count + " 个");
    }

    /**
     * 添加模型文件
     */
    private void addModels(ZipOutputStream zipOut, ModelGenerator modelGenerator) throws IOException {
        int count = 0;

        for (Map.Entry<String, String> entry : modelGenerator.getGeneratedModels().entrySet()) {
            String modelId = entry.getKey();  // ID已小写化
            String modelJson = entry.getValue();

            // 资源包中的路径: assets/guangdian/models/item/小写模型ID.json
            String zipPath = "assets/guangdian/models/item/" + modelId + ".json";

            ZipEntry zipEntry = new ZipEntry(zipPath);
            zipOut.putNextEntry(zipEntry);

            zipOut.write(modelJson.getBytes());

            zipOut.closeEntry();

            count++;
        }

        plugin.getLogger().info("模型添加完成，共 " + count + " 个");
    }

    /**
     * 添加基础物品的 overrides 模型文件（关键功能！）
     *
     * 修复：使用 CustomItemRegistry 作为 CustomModelData 的唯一来源，
     * 确保物品注册表和 overrides 中的 CMD 完全一致。
     * 不再独立遍历贴图并独立分配 CMD。
     */
    private void addBaseItemOverrides(ZipOutputStream zipOut, TextureManager textureManager) throws IOException {
        plugin.getLogger().info("开始生成基础物品 overrides 文件...");

        // 按 Material 分类物品定义（从注册表获取）
        Map<String, Map<Integer, String>> materialOverrides = new LinkedHashMap<>();

        // 基础材质映射：Material名称 → 资源包文件名
        Map<String, String> baseMaterials = new LinkedHashMap<>();
        baseMaterials.put("DIAMOND_SWORD", "diamond_sword");
        baseMaterials.put("DIAMOND_AXE", "diamond_axe");
        baseMaterials.put("BOW", "bow");
        baseMaterials.put("BLAZE_ROD", "blaze_rod");
        baseMaterials.put("TRIDENT", "trident");

        // ★ 需要跳过的分类（非武器，不加入overrides）
        Set<String> skipCategories = Set.of("称号", "魂环");

        // 从物品注册表遍历（唯一来源），确保 CMD 和材质映射一致
        if (itemRegistry != null) {
            for (CustomItemRegistry.CustomItemDefinition definition : itemRegistry.getAllDefinitions().values()) {
                // ★ 跳过非武器物品
                // 检查对应的贴图分类
                TextureManager.TextureInfo textureInfo = textureManager.getTexture(definition.getId());
                if (textureInfo != null && skipCategories.contains(textureInfo.getCategory())) {
                    continue;
                }

                String material = definition.getMaterial();
                String baseMaterialName = baseMaterials.getOrDefault(material, "diamond_sword");
                int customModelData = definition.getCustomModelData();
                String itemId = definition.getId();  // 已净化

                // 初始化该材质的 overrides 列表
                if (!materialOverrides.containsKey(baseMaterialName)) {
                    materialOverrides.put(baseMaterialName, new LinkedHashMap<>());
                }

                // 添加 overrides 映射：CMD → 模型ID（已净化，无非法字符）
                materialOverrides.get(baseMaterialName).put(customModelData, itemId);
            }
        }

        // 为每个材质生成 overrides 文件
        for (Map.Entry<String, Map<Integer, String>> entry : materialOverrides.entrySet()) {
            String baseMaterialName = entry.getKey();
            Map<Integer, String> overrides = entry.getValue();

            String overrideJson = generateOverrideJson(baseMaterialName, overrides);

            // 文件路径：assets/minecraft/models/item/diamond_sword.json
            String zipPath = "assets/minecraft/models/item/" + baseMaterialName + ".json";

            ZipEntry zipEntry = new ZipEntry(zipPath);
            zipOut.putNextEntry(zipEntry);
            zipOut.write(overrideJson.getBytes());
            zipOut.closeEntry();

            plugin.getLogger().info("已生成 " + baseMaterialName + ".json，包含 " + overrides.size() + " 个 overrides");
        }

        plugin.getLogger().info("基础物品 overrides 文件生成完成");
    }

    /**
     * 生成 overrides JSON 文件
     *
     * 修复：弓模型添加拉弓动画 overrides (pulling_0/1/2)
     */
    private String generateOverrideJson(String baseMaterialName, Map<Integer, String> overrides) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");

        // 基础模型
        if (baseMaterialName.equals("bow")) {
            json.append("  \"parent\": \"item/bow\",\n");
            json.append("  \"textures\": {\n");
            json.append("    \"layer0\": \"item/bow\"\n");
            json.append("  },\n");
        } else if (baseMaterialName.equals("blaze_rod")) {
            json.append("  \"parent\": \"item/handheld\",\n");
            json.append("  \"textures\": {\n");
            json.append("    \"layer0\": \"item/blaze_rod\"\n");
            json.append("  },\n");
        } else if (baseMaterialName.equals("trident")) {
            // 三叉戟（矛）使用专属模型父类
            json.append("  \"parent\": \"item/trident\",\n");
            json.append("  \"textures\": {\n");
            json.append("    \"layer0\": \"item/trident\"\n");
            json.append("  },\n");
        } else {
            json.append("  \"parent\": \"item/handheld\",\n");
            json.append("  \"textures\": {\n");
            json.append("    \"layer0\": \"item/" + baseMaterialName + "\"\n");
            json.append("  },\n");
        }

        // overrides 部分
        json.append("  \"overrides\": [\n");

        // 弓模型：添加拉弓动画基础 overrides (pulling 状态)
        if (baseMaterialName.equals("bow")) {
            json.append("    { \"predicate\": { \"pulling\": 1 }, \"model\": \"item/bow_pulling_0\" },\n");
            json.append("    { \"predicate\": { \"pulling\": 1, \"pull\": 0.65 }, \"model\": \"item/bow_pulling_1\" },\n");
            json.append("    { \"predicate\": { \"pulling\": 1, \"pull\": 0.9 }, \"model\": \"item/bow_pulling_2\" },\n");
        }

        int index = 0;
        for (Map.Entry<Integer, String> override : overrides.entrySet()) {
            int modelData = override.getKey();
            String modelId = override.getValue();  // 已小写化

            json.append("    {\n");
            json.append("      \"predicate\": { \"custom_model_data\": " + modelData + " },\n");
            json.append("      \"model\": \"guangdian:item/" + modelId + "\"\n");
            json.append("    }");

            if (index < overrides.size() - 1) {
                json.append(",");
            }
            json.append("\n");

            index++;
        }

        json.append("  ]\n");
        json.append("}\n");

        return json.toString();
    }

    /**
     * 清理临时文件
     */
    public void cleanupTempFiles(File outputDir) {
        File tempDir = new File(outputDir, "temp");
        if (tempDir.exists()) {
            deleteDirectory(tempDir);
            plugin.getLogger().info("临时文件已清理");
        }
    }

    /**
     * 删除目录
     */
    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }
}
