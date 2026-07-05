package cn.guangdian.mobs;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

/**
 * 怪物配置加载器 — MM 风格格式 (PascalCase)
 *
 * <pre>{@code
 * mob_id:
 *   Type: ZOMBIE
 *   Display: '<b>名称'
 *   Health: 3000
 *   Damage: 0
 *   Skills:
 *     - skill:技能名 @Target ~onTimer:100
 *     - damage{amount=100} @Target ~onTimer:60
 *     - command{c="rpgitem give <target.name> 物品 1"} @Trigger ~onDeath 0.1
 *     - potion{type=FIRE_RESISTANCE;duration=200;level=4} @Self ~onTimer:10
 *   Options:
 *     MovementSpeed: 0.15
 *     KnockbackResistance: 1
 *     AlwaysShowName: true
 *     PreventOtherDrops: true
 *   Drops:
 *     - exp 50
 *   Equipment:
 *     - 铁帽 HEAD
 *     - 铁剑 HAND
 *   BossBar:
 *     Enabled: true
 *     Color: RED
 *     Style: SOLID
 *     Title: '<mob.name>'
 *   DamageModifiers:
 *     - FIRE 0
 * }</pre>
 */
public class MobConfigLoader {

    private final Logger log;

    public MobConfigLoader(Logger log) { this.log = log; }

    public Map<String, MobTemplate> loadAll(File dataFolder) {
        Map<String, MobTemplate> map = new LinkedHashMap<>();
        scanDir(new File(dataFolder, "mobs"), map);
        // 兼容根 mobs.yml
        File main = new File(dataFolder, "mobs.yml");
        if (main.exists()) loadFile(main, map);
        log.info("加载 " + map.size() + " 怪物");
        return map;
    }

    private void scanDir(File dir, Map<String, MobTemplate> map) {
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files != null) for (File f : files) loadFile(f, map);
    }

    private void loadFile(File file, Map<String, MobTemplate> map) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String id : cfg.getKeys(false)) {
            ConfigurationSection s = cfg.getConfigurationSection(id);
            if (s == null || !s.contains("Type") && !s.contains("type")) continue;
            try {
                MobTemplate t = parse(id, s);
                if (t.isValid()) { map.put(id, t); log.info("  ✓ " + id); }
            } catch (Exception e) { log.warning("  ✗ " + id + ": " + e.getMessage()); }
        }
    }

    // ════════════════════════════════════════
    //  解析
    // ════════════════════════════════════════

    private MobTemplate parse(String id, ConfigurationSection s) {
        // 类型 (兼容 Type / type)
        String typeStr = or(s, "Type", "type", "ZOMBIE");
        EntityType type;
        try { type = EntityType.valueOf(typeStr.toUpperCase()); }
        catch (Exception e) { throw new IllegalArgumentException("无效类型: " + typeStr); }

        // 显示名 (兼容 Display / display)
        String display = or(s, "Display", "display", id);

        // 属性
        double health = orD(s, "Health", "health", 20);
        double damage = orD(s, "Damage", "damage", 3);
        double defense = orD(s, "Defense", "defense", 0);
        double speed = s.contains("Options") ? s.getConfigurationSection("Options").getDouble("MovementSpeed", 0.23) : orD(s, "speed", 0.23);
        double atkSpd = 1.0;
        double follow = orD(s, "FollowRange", "follow-range", 32);
        double kb = 0;

        // Options
        ConfigurationSection opts = s.getConfigurationSection("Options");
        boolean showName = opts != null && opts.getBoolean("AlwaysShowName", opts.getBoolean("always-show-name", false));
        boolean prevent = opts != null && opts.getBoolean("PreventOtherDrops", opts.getBoolean("prevent-other-drops", false));
        int size = opts != null ? opts.getInt("Size", opts.getInt("size", -1)) : -1;
        if (opts != null) {
            kb = opts.getDouble("KnockbackResistance", opts.getDouble("knockback-resistance", 0));
            follow = opts.getDouble("FollowRange", opts.getDouble("follow-range", follow));
        }

        // BossBar
        ConfigurationSection bb = s.getConfigurationSection("BossBar");
        MobTemplate.BossBar bossBar = MobTemplate.BossBar.NONE;
        if (bb != null && bb.getBoolean("Enabled", false)) {
            bossBar = new MobTemplate.BossBar(true,
                bb.getString("Color", "RED"),
                bb.getString("Style", "SOLID"),
                bb.getString("Title", "<mob.name>"));
        }

        // 技能行 - 支持两种格式
        List<MobTemplate.SkillLine> skills = new ArrayList<>();
        // 格式1: 内联技能格式 (Skills: ["damage{amount=10} @Target ~onTimer:100"])
        for (String line : s.getStringList("Skills")) {
            // 如果只是技能ID引用（如 "weak_attack"），转换为 skill{} 格式
            if (line.matches("^\\w+$") || line.matches("^\"\\w+\"$")) {
                String skillId = line.replace("\"", "").trim();
                skills.add(new MobTemplate.SkillLine("skill{s=" + skillId + "} @Target ~onTimer:100"));
            } else {
                skills.add(new MobTemplate.SkillLine(line));
            }
        }
        // 格式2: 属性格式 (skills: ["weak_attack"] 或 skills: [weak_attack])
        List<String> skillRefs = s.getStringList("skills");
        for (String skillId : skillRefs) {
            if (!skillId.isEmpty()) {
                // 检查是否已经是内联格式
                if (skillId.contains("{") || skillId.contains("@")) {
                    skills.add(new MobTemplate.SkillLine(skillId));
                } else {
                    // 转换为技能引用格式
                    skills.add(new MobTemplate.SkillLine("skill{s=" + skillId + "} @Target ~onTimer:100"));
                }
            }
        }

        // 装备 (原版 Material → RPGItems 物品)
        Map<String, ItemStack> eq = new LinkedHashMap<>();
        for (String line : s.getStringList("Equipment")) {
            String[] parts = line.split(" ");
            if (parts.length < 2) continue;
            String slotKey = parts[parts.length - 1].toUpperCase();
            // 还原物品原名（MM格式可能是中文名）
            String itemName = String.join(" ", Arrays.copyOf(parts, parts.length - 1));
            ItemStack item = resolveItem(itemName);
            if (item == null) continue;
            switch (slotKey) {
                case "HEAD" -> eq.put("head", item);
                case "CHEST" -> eq.put("chest", item);
                case "LEGS" -> eq.put("legs", item);
                case "FEET" -> eq.put("feet", item);
                case "HAND" -> eq.put("hand", item);
                case "OFFHAND" -> eq.put("offhand", item);
            }
        }

        // 伤害修正
        Map<String, Double> dm = new HashMap<>();
        for (String line : s.getStringList("DamageModifiers")) {
            String[] parts = line.split(" ");
            if (parts.length >= 2) {
                try { dm.put(parts[0].toUpperCase(), Double.parseDouble(parts[1])); }
                catch (NumberFormatException ignored) {}
            }
        }

        return new MobTemplate(id, display, type, s.getInt("Level", s.getInt("level", 1)),
            health, damage, defense, speed, atkSpd, follow, kb,
            eq, skills, dm,
            new MobTemplate.Opts(showName, prevent, size),
            bossBar);
    }

    // ════════════════════════════════════════
    //  物品解析: 英文原版 → 中文映射 → RPGItems
    // ════════════════════════════════════════

    private static Object rpgItemsService;

    private static ItemStack resolveItem(String name) {
        // 1. 英文原版 Material
        try {
            Material m = Material.valueOf(name.replace(" ", "_").toUpperCase());
            return new ItemStack(m);
        } catch (IllegalArgumentException ignored) {}

        // 2. 中文名 → 原版 Material 映射
        Material cn = CN_MATERIAL.get(name);
        if (cn != null) return new ItemStack(cn);

        // 3. RPGItems 自定义物品
        if (rpgItemsService == null) {
            try {
                var rpgPlugin = Bukkit.getPluginManager().getPlugin("RPGItems");
                if (rpgPlugin != null && rpgPlugin.isEnabled()) {
                    var cls = Class.forName("cn.guangdian.rpgitems.RPGItems");
                    var inst = cls.getMethod("getInstance").invoke(null);
                    rpgItemsService = cls.getMethod("getItemService").invoke(inst);
                }
            } catch (Exception e) { /* RPGItems 不可用 */ }
        }
        if (rpgItemsService != null) {
            try {
                @SuppressWarnings("unchecked")
                var opt = (java.util.Optional<ItemStack>) rpgItemsService.getClass()
                    .getMethod("createItem", String.class).invoke(rpgItemsService, name);
                if (opt != null && opt.isPresent()) return opt.get();
            } catch (Exception e) { /* 物品不存在 */ }
        }
        return null;
    }

    /** 中文装备名 → 原版 Material */
    private static final Map<String, Material> CN_MATERIAL = Map.ofEntries(
        Map.entry("皮帽", Material.LEATHER_HELMET),
        Map.entry("皮甲", Material.LEATHER_CHESTPLATE),
        Map.entry("皮裤", Material.LEATHER_LEGGINGS),
        Map.entry("皮靴", Material.LEATHER_BOOTS),
        Map.entry("铁帽", Material.IRON_HELMET),
        Map.entry("铁甲", Material.IRON_CHESTPLATE),
        Map.entry("铁裤", Material.IRON_LEGGINGS),
        Map.entry("铁靴", Material.IRON_BOOTS),
        Map.entry("铁剑", Material.IRON_SWORD),
        Map.entry("金帽", Material.GOLDEN_HELMET),
        Map.entry("金甲", Material.GOLDEN_CHESTPLATE),
        Map.entry("金裤", Material.GOLDEN_LEGGINGS),
        Map.entry("金靴", Material.GOLDEN_BOOTS),
        Map.entry("金斧", Material.GOLDEN_AXE),
        Map.entry("金剑", Material.GOLDEN_SWORD),
        Map.entry("钻石帽", Material.DIAMOND_HELMET),
        Map.entry("钻石甲", Material.DIAMOND_CHESTPLATE),
        Map.entry("钻石裤", Material.DIAMOND_LEGGINGS),
        Map.entry("钻石靴", Material.DIAMOND_BOOTS),
        Map.entry("钻石剑", Material.DIAMOND_SWORD),
        Map.entry("石剑", Material.STONE_SWORD),
        Map.entry("石斧", Material.STONE_AXE),
        Map.entry("木剑", Material.WOODEN_SWORD),
        Map.entry("木斧", Material.WOODEN_AXE),
        Map.entry("弓", Material.BOW),
        Map.entry("弩", Material.CROSSBOW),
        Map.entry("盾", Material.SHIELD),
        Map.entry("下界合金帽", Material.NETHERITE_HELMET),
        Map.entry("下界合金甲", Material.NETHERITE_CHESTPLATE),
        Map.entry("下界合金裤", Material.NETHERITE_LEGGINGS),
        Map.entry("下界合金靴", Material.NETHERITE_BOOTS),
        Map.entry("下界合金剑", Material.NETHERITE_SWORD),
        Map.entry("黑皮帽", Material.LEATHER_HELMET),
        Map.entry("黑皮甲", Material.LEATHER_CHESTPLATE),
        Map.entry("黑皮裤", Material.LEATHER_LEGGINGS),
        Map.entry("黑皮靴", Material.LEATHER_BOOTS)
    );

    private static String or(ConfigurationSection s, String a, String b, String def) {
        return s.contains(a) ? s.getString(a) : s.contains(b) ? s.getString(b) : s.contains(a.toLowerCase()) ? s.getString(a.toLowerCase()) : s.contains(b.toLowerCase()) ? s.getString(b.toLowerCase()) : def;
    }
    private static double orD(ConfigurationSection s, String a, String b, double def) {
        return s.contains(a) ? s.getDouble(a) : s.contains(b) ? s.getDouble(b) : def;
    }
    private static double orD(ConfigurationSection s, String a, double def) {
        return s.contains(a) ? s.getDouble(a) : def;
    }
}
