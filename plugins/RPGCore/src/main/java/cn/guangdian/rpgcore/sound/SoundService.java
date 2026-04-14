package cn.guangdian.rpgcore.sound;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class SoundService {

    private static SoundService instance;

    private SoundService() {}

    public static synchronized SoundService getInstance() {
        if (instance == null) {
            instance = new SoundService();
        }
        return instance;
    }

    public void playSound(Player player, String soundKey) {
        if (player == null || soundKey == null) return;
        Sound sound = createSound(soundKey, Sound.Source.PLAYER, 1.0f, 1.0f);
        player.playSound(sound);
    }

    public void playSound(Player player, String soundKey, float volume, float pitch) {
        if (player == null || soundKey == null) return;
        Sound sound = createSound(soundKey, Sound.Source.PLAYER, volume, pitch);
        player.playSound(sound);
    }

    public void playSound(Player player, String soundKey, Sound.Source source, float volume, float pitch) {
        if (player == null || soundKey == null) return;
        Sound sound = createSound(soundKey, source, volume, pitch);
        player.playSound(sound);
    }

    public void playSound(Player player, Location location, String soundKey, float volume, float pitch) {
        if (player == null || location == null || soundKey == null) return;
        Sound sound = createSound(soundKey, Sound.Source.PLAYER, volume, pitch);
        player.playSound(sound, location.getX(), location.getY(), location.getZ());
    }

    public void playSoundFollow(Player player, String soundKey, float volume, float pitch) {
        if (player == null || soundKey == null) return;
        Sound sound = createSound(soundKey, Sound.Source.PLAYER, volume, pitch);
        player.playSound(sound, Sound.Emitter.self());
    }

    public void stopSound(Player player, String soundKey) {
        if (player == null || soundKey == null) return;
        SoundStop stop = SoundStop.named(Key.key(soundKey));
        player.stopSound(stop);
    }

    public void stopSound(Player player, Sound.Source source) {
        if (player == null || source == null) return;
        SoundStop stop = SoundStop.source(source);
        player.stopSound(stop);
    }

    public void stopAllSounds(Player player) {
        if (player == null) return;
        player.stopSound(SoundStop.all());
    }

    public void broadcastSound(String soundKey, float volume, float pitch) {
        if (soundKey == null) return;
        Sound sound = createSound(soundKey, Sound.Source.MASTER, volume, pitch);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(sound);
        }
    }

    public void broadcastSound(Location location, String soundKey, float volume, float pitch) {
        if (location == null || soundKey == null) return;
        Sound sound = createSound(soundKey, Sound.Source.MASTER, volume, pitch);
        double radius = volume * 10;
        double radiusSq = radius * radius;
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= radiusSq) {
                player.playSound(sound, location.getX(), location.getY(), location.getZ());
            }
        }
    }

    public void playUISound(Player player) {
        playSound(player, "minecraft:ui.button.click", 0.5f, 1.0f);
    }

    public void playSuccessSound(Player player) {
        playSound(player, "minecraft:entity.player.levelup", 0.5f, 1.2f);
    }

    public void playErrorSound(Player player) {
        playSound(player, "minecraft:entity.villager.no", 0.5f, 1.0f);
    }

    public void playPickupSound(Player player) {
        playSound(player, "minecraft:entity.item.pickup", 0.5f, 1.0f);
    }

    public void playHitSound(Player player) {
        playSound(player, "minecraft:entity.player.hurt", 0.5f, 1.0f);
    }

    private Sound createSound(String soundKey, Sound.Source source, float volume, float pitch) {
        Key key;
        if (soundKey.contains(":")) {
            key = Key.key(soundKey);
        } else {
            key = Key.key("minecraft", soundKey);
        }
        return Sound.sound(key, source, volume, clampPitch(pitch));
    }

    private float clampPitch(float pitch) {
        return Math.max(0.5f, Math.min(2.0f, pitch));
    }

    public static SoundStop stopAll() {
        return SoundStop.all();
    }

    public static SoundStop stopNamed(String soundKey) {
        return SoundStop.named(Key.key(soundKey));
    }

    public static SoundStop stopSource(Sound.Source source) {
        return SoundStop.source(source);
    }
}
