package cn.guangdian.mobs.skills.condition;

import cn.guangdian.mobs.model.MobSkill;
import org.bukkit.Registry;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.entity.LivingEntity;

/**
 * 生物群系条件 - 检查施法者是否处于指定生物群系
 * 格式: biome <生物群系名称>  例: biome DARK_FOREST
 */
public class BiomeCondition implements SkillCondition {

    private final Biome requiredBiome;

    public BiomeCondition(String biomeName) {
        Biome biome = null;
        try {
            // 新版 API 使用 Registry
            biome = Registry.BIOME.get(NamespacedKey.minecraft(biomeName.toLowerCase()));
        } catch (IllegalArgumentException ignored) {}
        this.requiredBiome = biome;
    }

    @Override
    public boolean check(LivingEntity caster, LivingEntity target, MobSkill skill) {
        if (requiredBiome == null) return true;
        return caster.getLocation().getBlock().getBiome() == requiredBiome;
    }

    @Override
    public String getType() {
        return "BIOME";
    }
}
