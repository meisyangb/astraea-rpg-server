package cn.guangdian.custommodels.texture;

import cn.guangdian.custommodels.config.CustomModelsConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

/**
 * 贴图管理器
 * 扫描和管理所有贴图文件
 *
 * 修复：
 * 1. ID净化 — 去除空格/括号/#等非法字符，确保Minecraft命名空间合法性 [a-z0-9_.-]
 * 2. 子目录分类 — 利用父目录名辅助分类（称号/魂环/贴图1~7等）
 * 3. 过滤非武器贴图 — .gif/.mcmeta 不作为武器模型贴图
 * 4. 扩展名小写化 — ZIP中统一使用 .png 扩展名
 * 5. ID去重 — 相同净化ID后缀追加数字避免冲突
 */
public class TextureManager {

    private final JavaPlugin plugin;
    private final CustomModelsConfig config;
    private final Map<String, TextureInfo> textureMap = new LinkedHashMap<>();
    private final Map<String, List<TextureInfo>> categoryMap = new LinkedHashMap<>();
    private final Set<String> usedIds = new LinkedHashSet<>();  // 用于ID去重

    public TextureManager(JavaPlugin plugin, CustomModelsConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * 扫描贴图目录（递归扫描所有子文件夹）
     */
    public void scanTextures() {
        String sourceDir = config.getTextureSourceDirectory();
        File textureDir = new File(sourceDir);

        if (!textureDir.exists() || !textureDir.isDirectory()) {
            plugin.getLogger().warning("贴图目录不存在: " + sourceDir);
            return;
        }

        plugin.getLogger().info("正在递归扫描贴图目录: " + sourceDir);

        textureMap.clear();
        categoryMap.clear();
        usedIds.clear();

        int count = scanDirectory(textureDir, textureDir.getName());

        plugin.getLogger().info("扫描完成，共发现 " + count + " 个贴图");
        printStatistics();
    }

    /**
     * 递归扫描目录，传递父目录名用于分类
     */
    private int scanDirectory(File directory, String parentDirName) {
        int count = 0;

        File[] files = directory.listFiles();
        if (files == null) {
            return count;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // 递归扫描子目录，传递子目录名
                count += scanDirectory(file, file.getName());
            } else if (isWeaponTextureFile(file.getName())) {
                // 只处理武器类贴图（排除gif/mcmeta等）
                try {
                    TextureInfo info = analyzeTexture(file, parentDirName);
                    if (info != null) {
                        textureMap.put(info.getId(), info);

                        // 添加到分类
                        String category = info.getCategory();
                        categoryMap.computeIfAbsent(category, k -> new ArrayList<>()).add(info);

                        count++;
                        if (count % 500 == 0) {
                            plugin.getLogger().info("已扫描 " + count + " 个贴图");
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("处理贴图失败: " + file.getName() + " - " + e.getMessage());
                }
            }
        }

        return count;
    }

    /**
     * 判断是否为武器贴图文件
     * 仅接受 png/jpg/jpeg，排除 gif/mcmeta 等非武器贴图
     */
    private boolean isWeaponTextureFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        // 只接受PNG和JPG作为武器贴图
        if (lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            // 排除mcmeta动画描述文件
            if (lowerName.endsWith(".mcmeta")) return false;
            return true;
        }
        return false;
    }

    /**
     * 分析贴图文件
     *
     * 关键修复：净化ID，去除所有非法字符
     */
    private TextureInfo analyzeTexture(File file, String parentDirName) {
        String fileName = file.getName();

        // 移除文件扩展名
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = fileName.substring(0, dotIndex);

        // 扩展名小写化（ZIP中统一使用.png）
        String extension = fileName.substring(dotIndex).toLowerCase();
        // JPG/JPEG统一转为png概念（保存时仍用原文件，但ZIP路径用.png）
        if (extension.equals(".jpg") || extension.equals(".jpeg")) {
            extension = ".png";
        }

        // ★ 关键修复：净化ID — 只保留 [a-z0-9_.-]
        // 先小写化，再去除非法字符
        String rawId = baseName.toLowerCase();
        String sanitizedId = sanitizeId(rawId);

        // ID去重 — 如果净化后的ID已被使用，追加数字后缀
        String finalId = ensureUniqueId(sanitizedId);

        // 解析贴图ID和分类
        TextureInfo info = new TextureInfo();
        info.setId(finalId);
        info.setOriginalFileName(fileName);
        info.setSanitizedFileName(finalId + extension);  // ZIP中使用的净化文件名
        info.setFilePath(file.getAbsolutePath());
        info.setSize(file.length());

        // 分类分析 — 使用原始文件名+父目录名综合判断
        String category = parseCategory(baseName, parentDirName);
        info.setCategory(category);

        // 状态分析（如 ICON1_1, ICON1_2）
        String state = parseState(baseName);
        info.setState(state);

        return info;
    }

    /**
     * ★ 净化ID — 去除所有Minecraft命名空间不允许的字符
     * 只保留 [a-z0-9_.-]，其他字符替换为下划线或删除
     */
    private String sanitizeId(String rawId) {
        StringBuilder sb = new StringBuilder();
        for (char c : rawId.toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.' || c == '-') {
                sb.append(c);
            } else if (c == ' ') {
                // 空格替换为下划线
                sb.append('_');
            } else if (c == '(' || c == ')' || c == '#' || c == '[' || c == ']' || c == '{' || c == '}') {
                // 括号/#等特殊字符直接删除（不替换，避免ID过长）
                // 不追加任何字符
            } else if (c >= 'A' && c <= 'Z') {
                // 大写字母转小写（虽然rawId已经小写化了，保险起见）
                sb.append(Character.toLowerCase(c));
            } else if (c >= 0x4E00 && c <= 0x9FFF) {
                // 中文字符 — 删除（Minecraft不支持中文路径）
                // 不追加任何字符
            } else {
                // 其他非法字符替换为下划线
                sb.append('_');
            }
        }

        // 清理连续下划线
        String result = sb.toString();
        while (result.contains("__")) {
            result = result.replace("__", "_");
        }

        // 清理前导/尾随下划线
        result = result.trim();
        while (result.startsWith("_")) {
            result = result.substring(1);
        }
        while (result.endsWith("_")) {
            result = result.substring(0, result.length() - 1);
        }

        // 确保ID不为空
        if (result.isEmpty()) {
            result = "unknown_" + System.nanoTime();
        }

        return result;
    }

    /**
     * 确保ID唯一 — 如果净化后的ID已被使用，追加数字后缀
     */
    private String ensureUniqueId(String sanitizedId) {
        if (!usedIds.contains(sanitizedId)) {
            usedIds.add(sanitizedId);
            return sanitizedId;
        }

        // ID冲突，追加数字
        int counter = 2;
        String newId;
        do {
            newId = sanitizedId + "_" + counter;
            counter++;
        } while (usedIds.contains(newId));

        usedIds.add(newId);
        plugin.getLogger().warning("ID冲突: '" + sanitizedId + "' → 重命名为 '" + newId + "'");
        return newId;
    }

    /**
     * 解析贴图分类 — 综合使用文件名关键词和父目录名
     */
    private String parseCategory(String baseName, String parentDirName) {
        // 1. 优先使用文件名中的武器分类关键词
        List<String> categories = config.getTextureCategories();

        for (String category : categories) {
            if (baseName.contains(category)) {
                return category;
            }
        }

        // 2. 职业武器特殊处理
        if (baseName.contains("BingFaShi")) return "SPR";    // 冰法师 → 矛
        if (baseName.contains("FaShi")) return "STF";         // 法师 → 法杖
        if (baseName.contains("KuangZhanShi")) return "AXE";  // 狂战 → 斧
        if (baseName.contains("WuShi")) return "SD";          // 武士 → 剑
        if (baseName.contains("YanShuShi")) return "SD";      // 岩术 → 剑
        if (baseName.contains("YuWei")) return "SD";          // 御卫 → 剑

        // 3. NPC武器 → 剑类
        if (baseName.startsWith("npc") || baseName.startsWith("Npc")) return "SD";

        // 4. 根据父目录名分类
        // "整理完的贴图" → 可能包含各类武器，默认剑
        // "贴图1" → NPC类
        // "贴图2" → 职业武器类
        // "贴图3~7" → 武器类
        // "称号" → 非武器，跳过
        // "魂环" → 非武器，跳过
        switch (parentDirName) {
            case "称号":
                return "称号";  // 非武器
            case "魂环":
                return "魂环";  // 非武器
            case "贴图1":
                return "SD_NPC";  // NPC武器 → 剑
            case "贴图2":
                return "SD_JOB";  // 职业武器 → 剑(大多)
            default:
                // "贴图3~7", "整理完的贴图" 等 → 默认剑类
                if (parentDirName.startsWith("贴图")) return "SD";
                if (parentDirName.equals("整理完的贴图")) return "SD";
                break;
        }

        // 5. 兜底 — 默认剑类
        return "SD";
    }

    /**
     * 解析贴图状态
     * 例如: 1SD100003_ICON1_1 -> ICON1_1
     */
    private String parseState(String baseName) {
        if (baseName.contains("_ICON")) {
            int iconIndex = baseName.indexOf("_ICON");
            return baseName.substring(iconIndex);
        }
        return "default";
    }

    /**
     * 打印统计信息
     */
    private void printStatistics() {
        plugin.getLogger().info("========== 贴图统计 ==========");
        for (Map.Entry<String, List<TextureInfo>> entry : categoryMap.entrySet()) {
            plugin.getLogger().info(entry.getKey() + ": " + entry.getValue().size() + " 个");
        }
        plugin.getLogger().info("================================");
    }

    /**
     * 清理缓存
     */
    public void clearCache() {
        textureMap.clear();
        categoryMap.clear();
        usedIds.clear();
    }

    // Getter 方法
    public int getTextureCount() {
        return textureMap.size();
    }

    public TextureInfo getTexture(String id) {
        return textureMap.get(id);
    }

    public Map<String, TextureInfo> getAllTextures() {
        return new LinkedHashMap<>(textureMap);
    }

    public List<TextureInfo> getTexturesByCategory(String category) {
        return categoryMap.getOrDefault(category, new ArrayList<>());
    }

    public Map<String, List<TextureInfo>> getCategoryMap() {
        return new LinkedHashMap<>(categoryMap);
    }

    /**
     * 贴图信息类
     */
    public static class TextureInfo {
        private String id;              // 净化后的唯一ID（用于命名空间路径）
        private String originalFileName; // 原始文件名（用于读取源文件）
        private String sanitizedFileName; // 净化后的文件名（用于ZIP内路径）
        private String filePath;        // 源文件绝对路径
        private long size;
        private String category;
        private String state;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        /** 原始文件名（磁盘上的文件名） */
        public String getOriginalFileName() {
            return originalFileName;
        }

        public void setOriginalFileName(String originalFileName) {
            this.originalFileName = originalFileName;
        }

        /** 净化后的文件名（ZIP内的文件名） */
        public String getSanitizedFileName() {
            return sanitizedFileName;
        }

        public void setSanitizedFileName(String sanitizedFileName) {
            this.sanitizedFileName = sanitizedFileName;
        }

        /**
         * @deprecated 使用 getOriginalFileName() 或 getSanitizedFileName() 替代
         */
        @Deprecated
        public String getFileName() {
            return originalFileName;
        }

        public void setFileName(String fileName) {
            this.originalFileName = fileName;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        @Override
        public String toString() {
            return "TextureInfo{" +
                    "id='" + id + '\'' +
                    ", originalFileName='" + originalFileName + '\'' +
                    ", sanitizedFileName='" + sanitizedFileName + '\'' +
                    ", category='" + category + '\'' +
                    ", state='" + state + '\'' +
                    ", size=" + size +
                    '}';
        }
    }
}
