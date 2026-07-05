package cn.guangdian.custommodels.model;

import cn.guangdian.custommodels.config.CustomModelsConfig;
import cn.guangdian.custommodels.texture.TextureManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模型生成器
 * 为贴图生成对应的模型JSON文件
 */
public class ModelGenerator {

    private final JavaPlugin plugin;
    private final CustomModelsConfig config;
    private final Map<String, String> generatedModels = new LinkedHashMap<>();

    public ModelGenerator(JavaPlugin plugin, CustomModelsConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * 为所有贴图生成模型
     */
    public void generateModels(TextureManager textureManager) {
        Map<String, TextureManager.TextureInfo> textures = textureManager.getAllTextures();

        plugin.getLogger().info("开始生成模型，共 " + textures.size() + " 个贴图");

        generatedModels.clear();

        int count = 0;
        int skipped = 0;
        for (TextureManager.TextureInfo texture : textures.values()) {
            // ★ 跳过非武器类贴图（称号、魂环等不生成模型）
            String category = texture.getCategory();
            if (category.equals("称号") || category.equals("魂环")) {
                skipped++;
                continue;
            }

            try {
                String modelJson = generateModelJson(texture);
                generatedModels.put(texture.getId(), modelJson);

                count++;
                if (count % 500 == 0) {
                    plugin.getLogger().info("已生成 " + count + " 个模型");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("生成模型失败: " + texture.getId() + " - " + e.getMessage());
            }
        }

        if (skipped > 0) {
            plugin.getLogger().info("跳过非武器贴图 " + skipped + " 个");
        }
        plugin.getLogger().info("模型生成完成，共 " + count + " 个");
    }

    /**
     * 生成单个贴图的模型JSON
     */
    private String generateModelJson(TextureManager.TextureInfo texture) {
        String mode = config.getModelGenerationMode();

        if ("2d".equalsIgnoreCase(mode)) {
            return generate2DModel(texture);
        } else if ("3d".equalsIgnoreCase(mode)) {
            return generate3DModel(texture);
        }

        return generate2DModel(texture); // 默认使用2D
    }

    /**
     * 生成2D平面模型
     */
    private String generate2DModel(TextureManager.TextureInfo texture) {
        // 贴图ID已小写化，直接使用即可
        String texturePath = "guangdian:weapons/" + texture.getId();

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"parent\": \"item/generated\",\n");
        json.append("  \"textures\": {\n");
        json.append("    \"layer0\": \"" + texturePath + "\"\n");
        json.append("  }\n");
        json.append("}\n");

        return json.toString();
    }

    /**
     * 生成3D立体模型
     */
    private String generate3DModel(TextureManager.TextureInfo texture) {
        // 贴图ID已小写化，直接使用即可
        String texturePath = "guangdian:weapons/" + texture.getId();
        String category = texture.getCategory();

        // 根据武器类型生成不同的模型
        if (category.contains("SD") || category.contains("职业武器-武士")) {
            return generateSwordModel(texturePath);
        } else if (category.contains("AXE") || category.contains("职业武器-狂战")) {
            return generateAxeModel(texturePath);
        } else if (category.contains("BOW")) {
            return generateBowModel(texturePath);
        } else if (category.contains("SPR") || category.contains("职业武器-冰法")) {
            return generateSpearModel(texturePath);
        } else if (category.contains("STF") || category.contains("职业武器-法师")) {
            return generateStaffModel(texturePath);
        } else if (category.contains("DGR")) {
            return generateDaggerModel(texturePath);
        }

        // 默认使用剑模型
        return generateSwordModel(texturePath);
    }

    /**
     * 剑模型模板
     */
    private String generateSwordModel(String texturePath) {
        return "{\n" +
                "  \"parent\": \"item/generated\",\n" +
                "  \"textures\": {\n" +
                "    \"layer0\": \"" + texturePath + "\"\n" +
                "  },\n" +
                "  \"display\": {\n" +
                "    \"thirdperson_righthand\": {\n" +
                "      \"rotation\": [0, 90, -35],\n" +
                "      \"translation\": [0, 1.6, -1.9],\n" +
                "      \"scale\": [0.63, 0.63, 0.63]\n" +
                "    },\n" +
                "    \"firstperson_righthand\": {\n" +
                "      \"rotation\": [0, -90, 25],\n" +
                "      \"translation\": [1.13, 3.2, 1.13],\n" +
                "      \"scale\": [0.68, 0.68, 0.68]\n" +
                "    },\n" +
                "    \"gui\": {\n" +
                "      \"rotation\": [0, 0, 0],\n" +
                "      \"translation\": [0, 0, 0],\n" +
                "      \"scale\": [1, 1, 1]\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
    }

    /**
     * 斧模型模板
     */
    private String generateAxeModel(String texturePath) {
        return "{\n" +
                "  \"parent\": \"item/generated\",\n" +
                "  \"textures\": {\n" +
                "    \"layer0\": \"" + texturePath + "\"\n" +
                "  },\n" +
                "  \"display\": {\n" +
                "    \"thirdperson_righthand\": {\n" +
                "      \"rotation\": [0, 90, -35],\n" +
                "      \"translation\": [0, 1.5, -1.5],\n" +
                "      \"scale\": [0.65, 0.65, 0.65]\n" +
                "    },\n" +
                "    \"firstperson_righthand\": {\n" +
                "      \"rotation\": [0, -90, 25],\n" +
                "      \"translation\": [1.13, 3.2, 1.13],\n" +
                "      \"scale\": [0.68, 0.68, 0.68]\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
    }

    /**
     * 弓模型模板
     */
    private String generateBowModel(String texturePath) {
        return "{\n" +
                "  \"parent\": \"item/generated\",\n" +
                "  \"textures\": {\n" +
                "    \"layer0\": \"" + texturePath + "\"\n" +
                "  },\n" +
                "  \"display\": {\n" +
                "    \"thirdperson_righthand\": {\n" +
                "      \"rotation\": [-80, 260, -40],\n" +
                "      \"translation\": [-1, 2, 0.5],\n" +
                "      \"scale\": [0.9, 0.9, 0.9]\n" +
                "    },\n" +
                "    \"firstperson_righthand\": {\n" +
                "      \"rotation\": [-80, 260, -40],\n" +
                "      \"translation\": [-1, 2, 0.5],\n" +
                "      \"scale\": [0.9, 0.9, 0.9]\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
    }

    /**
     * 矛模型模板
     */
    private String generateSpearModel(String texturePath) {
        return "{\n" +
                "  \"parent\": \"item/generated\",\n" +
                "  \"textures\": {\n" +
                "    \"layer0\": \"" + texturePath + "\"\n" +
                "  },\n" +
                "  \"display\": {\n" +
                "    \"thirdperson_righthand\": {\n" +
                "      \"rotation\": [0, 90, -55],\n" +
                "      \"translation\": [0, 2.5, -5],\n" +
                "      \"scale\": [0.8, 0.8, 0.8]\n" +
                "    },\n" +
                "    \"firstperson_righthand\": {\n" +
                "      \"rotation\": [0, -90, 25],\n" +
                "      \"translation\": [1.13, 3.2, 1.13],\n" +
                "      \"scale\": [0.68, 0.68, 0.68]\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
    }

    /**
     * 法杖模型模板
     */
    private String generateStaffModel(String texturePath) {
        return "{\n" +
                "  \"parent\": \"item/generated\",\n" +
                "  \"textures\": {\n" +
                "    \"layer0\": \"" + texturePath + "\"\n" +
                "  },\n" +
                "  \"display\": {\n" +
                "    \"thirdperson_righthand\": {\n" +
                "      \"rotation\": [0, 0, 0],\n" +
                "      \"translation\": [0, 2, -5],\n" +
                "      \"scale\": [0.7, 0.7, 0.7]\n" +
                "    },\n" +
                "    \"firstperson_righthand\": {\n" +
                "      \"rotation\": [0, -90, 45],\n" +
                "      \"translation\": [0, 1.6, 1.6],\n" +
                "      \"scale\": [0.68, 0.68, 0.68]\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
    }

    /**
     * 匕首模型模板
     */
    private String generateDaggerModel(String texturePath) {
        return "{\n" +
                "  \"parent\": \"item/generated\",\n" +
                "  \"textures\": {\n" +
                "    \"layer0\": \"" + texturePath + "\"\n" +
                "  },\n" +
                "  \"display\": {\n" +
                "    \"thirdperson_righthand\": {\n" +
                "      \"rotation\": [0, 90, -35],\n" +
                "      \"translation\": [0, 1.2, -1.2],\n" +
                "      \"scale\": [0.55, 0.55, 0.55]\n" +
                "    },\n" +
                "    \"firstperson_righthand\": {\n" +
                "      \"rotation\": [0, -90, 25],\n" +
                "      \"translation\": [1.13, 3.2, 1.13],\n" +
                "      \"scale\": [0.68, 0.68, 0.68]\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
    }

    /**
     * 保存模型JSON文件
     */
    public void saveModelJson(String modelId, String modelJson, File outputDir) {
        File modelFile = new File(outputDir, modelId + ".json");

        try (FileWriter writer = new FileWriter(modelFile)) {
            writer.write(modelJson);
            plugin.getLogger().info("模型已保存: " + modelFile.getAbsolutePath());
        } catch (IOException e) {
            plugin.getLogger().severe("保存模型失败: " + modelId + " - " + e.getMessage());
        }
    }

    // Getter 方法
    public Map<String, String> getGeneratedModels() {
        return new LinkedHashMap<>(generatedModels);
    }

    public String getModel(String id) {
        return generatedModels.get(id);
    }

    public int getModelCount() {
        return generatedModels.size();
    }
}