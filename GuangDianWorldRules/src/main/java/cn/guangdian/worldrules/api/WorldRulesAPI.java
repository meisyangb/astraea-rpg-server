package cn.guangdian.worldrules.api;

import cn.guangdian.worldrules.model.WorldRules;
import org.bukkit.World;

import java.util.Set;

public interface WorldRulesAPI {

    /**
     * 获取世界的规则配置
     */
    WorldRules getWorldRules(String worldName);

    /**
     * 获取世界的规则配置
     */
    WorldRules getWorldRules(World world);

    /**
     * 检查世界是否已配置规则
     */
    boolean hasWorldRules(String worldName);

    /**
     * 获取默认规则
     */
    WorldRules getDefaultRules();

    /**
     * 获取所有已配置规则的世界名称
     */
    Set<String> getConfiguredWorlds();

    /**
     * 检查世界是否开启死亡不掉落
     */
    boolean isKeepInventory(String worldName);

    /**
     * 检查世界是否开启死亡不掉经验
     */
    boolean isKeepExp(String worldName);

    /**
     * 检查世界是否禁止自然刷新生物
     */
    boolean isDisableNaturalSpawn(String worldName);

    /**
     * 检查世界是否禁止怪物刷新
     */
    boolean isDisableMonsterSpawn(String worldName);

    /**
     * 检查世界是否禁止动物刷新
     */
    boolean isDisableAnimalSpawn(String worldName);

    /**
     * 检查世界是否禁止PVP
     */
    boolean isPvpDisabled(String worldName);

    /**
     * 检查世界是否允许某种生物刷新
     */
    boolean canMobSpawn(String worldName, String mobType);

    /**
     * 重新加载所有规则
     */
    void reloadRules();

    /**
     * 重新加载特定世界的规则
     */
    void reloadWorldRules(String worldName);
}
