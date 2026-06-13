package cn.guangdian.rpgcore.sound;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * RPGCore 音效服务
 * 
 * 封装 Paper 1.21.6 Sound API，提供统一的音效播放接口
 * 解决 Sound.valueOf() 和 Sound.key() 弃用问题
 */
public final class SoundService {

    private static SoundService instance;
    
    // 常用音效名称映射 (命名空间 -> Key)
    private final Map<String, Key> commonSounds = new HashMap<>();

    private SoundService() {
        initCommonSounds();
    }
    
    private void initCommonSounds() {
        // UI 音效
        commonSounds.put("CLICK", Key.key("minecraft:ui.button.click"));
        commonSounds.put("BUTTON_CLICK", Key.key("minecraft:ui.button.click"));
        
        // 成功/失败音效
        commonSounds.put("SUCCESS", Key.key("minecraft:entity.player.levelup"));
        commonSounds.put("LEVEL_UP", Key.key("minecraft:entity.player.levelup"));
        commonSounds.put("ERROR", Key.key("minecraft:entity.villager.no"));
        commonSounds.put("NO", Key.key("minecraft:entity.villager.no"));
        
        // 物品音效
        commonSounds.put("PICKUP", Key.key("minecraft:entity.item.pickup"));
        commonSounds.put("ITEM_PICKUP", Key.key("minecraft:entity.item.pickup"));
        commonSounds.put("BREAK", Key.key("minecraft:entity.item.break"));
        commonSounds.put("ITEM_BREAK", Key.key("minecraft:entity.item.break"));
        
        // 战斗音效
        commonSounds.put("HIT", Key.key("minecraft:entity.player.hurt"));
        commonSounds.put("HURT", Key.key("minecraft:entity.player.hurt"));
        commonSounds.put("ATTACK", Key.key("minecraft:entity.player.attack.strong"));
        
        // 音符盒音效
        commonSounds.put("NOTE_PLING", Key.key("minecraft:block.note_block.pling"));
        commonSounds.put("NOTE_HARP", Key.key("minecraft:block.note_block.harp"));
        commonSounds.put("NOTE_BASS", Key.key("minecraft:block.note_block.bass"));
        
        // 门/箱子音效
        commonSounds.put("CHEST_OPEN", Key.key("minecraft:block.chest.open"));
        commonSounds.put("CHEST_CLOSE", Key.key("minecraft:block.chest.close"));
        commonSounds.put("DOOR_OPEN", Key.key("minecraft:block.wooden_door.open"));
        commonSounds.put("DOOR_CLOSE", Key.key("minecraft:block.wooden_door.close"));
        
        // 经验音效
        commonSounds.put("EXP", Key.key("minecraft:entity.experience_orb.pickup"));
        commonSounds.put("EXP_ORB", Key.key("minecraft:entity.experience_orb.pickup"));
        
        // 交易音效
        commonSounds.put("TRADE", Key.key("minecraft:entity.villager.trade"));
        commonSounds.put("TRADE_YES", Key.key("minecraft:entity.villager.yes"));
        
        // 传送音效
        commonSounds.put("TELEPORT", Key.key("minecraft:item.chorus_fruit.teleport"));
        commonSounds.put("PORTAL", Key.key("minecraft:block.portal.travel"));
        
        // 施法音效
        commonSounds.put("SPELL", Key.key("minecraft:entity.evoker.cast_spell"));
        commonSounds.put("CAST_SPELL", Key.key("minecraft:entity.evoker.cast_spell"));
        commonSounds.put("PREPARE_SPELL", Key.key("minecraft:entity.evoker.prepare_summon"));
        
        // 金币音效
        commonSounds.put("COIN", Key.key("minecraft:entity.experience_orb.pickup"));
        commonSounds.put("MONEY", Key.key("minecraft:entity.experience_orb.pickup"));
    }

    public static synchronized SoundService getInstance() {
        if (instance == null) {
            instance = new SoundService();
        }
        return instance;
    }
    
    /**
     * 解析音效名称（解决 Sound.valueOf() 弃用问题）
     * 
     * @param soundName 音效名称，支持：
     *                   - 完整命名空间: "minecraft:ui.button.click"
     *                   - 简写名称: "ui.button.click"
     *                   - 常用别名: "CLICK", "SUCCESS", "ERROR" 等
     * @return 解析后的音效 Key，如果解析失败返回 null
     */
    public Key parseSound(String soundName) {
        if (soundName == null || soundName.isEmpty()) {
            return null;
        }
        
        String upperName = soundName.toUpperCase();
        
        // 1. 先检查常用别名
        Key mapped = commonSounds.get(upperName);
        if (mapped != null) {
            return mapped;
        }
        
        // 2. 解析完整命名空间
        try {
            if (soundName.contains(":")) {
                return Key.key(soundName);
            } else {
                return Key.key("minecraft", soundName);
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取音效的完整键名（解决 Sound.key() 弃用问题）
     * 
     * @param soundKey 音效 Key
     * @return 音效的完整键名，如 "minecraft:ui.button.click"
     */
    public String getSoundKey(Key soundKey) {
        if (soundKey == null) return null;
        return soundKey.asString();
    }
    
    /**
     * 根据音效名称创建 Adventure Sound 对象
     */
    public Sound createAdventureSound(String soundName, Sound.Source source, float volume, float pitch) {
        Key key = parseSound(soundName);
        if (key == null) return null;
        return Sound.sound(key, source, volume, clampPitch(pitch));
    }
    
    /**
     * 播放音效给指定玩家
     */
    public void playSound(Player player, String soundName, float volume, float pitch) {
        if (player == null) return;
        
        Key key = parseSound(soundName);
        if (key == null) return;
        
        Sound sound = Sound.sound(key, Sound.Source.MASTER, volume, clampPitch(pitch));
        player.playSound(sound);
    }
    
    /**
     * 在指定位置播放音效
     */
    public void playSound(Location location, String soundName, float volume, float pitch) {
        if (location == null || location.getWorld() == null) return;
        
        Key key = parseSound(soundName);
        if (key == null) return;
        
        Sound sound = Sound.sound(key, Sound.Source.MASTER, volume, clampPitch(pitch));
        location.getWorld().playSound(sound, location.getX(), location.getY(), location.getZ());
    }
    
    /**
     * 播放音效给所有玩家
     */
    public void broadcastSound(String soundName, float volume, float pitch) {
        Key key = parseSound(soundName);
        if (key == null) return;
        
        Sound sound = Sound.sound(key, Sound.Source.MASTER, volume, clampPitch(pitch));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(sound);
        }
    }
    
    /**
     * 停止指定音效
     */
    public void stopSound(Player player, String soundName) {
        if (player == null) return;
        
        Key key = parseSound(soundName);
        if (key == null) return;
        
        player.stopSound(SoundStop.named(key));
    }
    
    /**
     * 停止所有音效
     */
    public void stopAllSounds(Player player) {
        if (player == null) return;
        player.stopSound(SoundStop.all());
    }

    private float clampPitch(float pitch) {
        return Math.max(0.5f, Math.min(2.0f, pitch));
    }
}
