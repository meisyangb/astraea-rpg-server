package cn.guangdian.rpgcore.service.api;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

public interface TextDisplayService {

    void createHologram(String id, Location location, Component text);

    void createHologram(String id, Location location, Component text, TextDisplayOptions options);

    void updateHologram(String id, Component text);

    void updateHologramLocation(String id, Location location);

    void removeHologram(String id);

    void removeHologram(String id, boolean playEffect);

    void removeAllHolograms();

    boolean hasHologram(String id);

    void showHologramToPlayer(String id, Player player);

    void hideHologramFromPlayer(String id, Player player);

    void showHologramToPlayers(String id, Collection<? extends Player> players);

    void hideHologramFromPlayers(String id, Collection<? extends Player> players);

    void setHologramVisibleToAll(String id, boolean visible);

    UUID getHologramEntityId(String id);

    Location getHologramLocation(String id);

    int getHologramCount();

    void clearAll();

    boolean isAvailable();

    class TextDisplayOptions {
        private boolean billboard = true;
        private boolean shadowed = true;
        private boolean seeThrough = false;
        private float lineWidth = 200f;
        private int backgroundColor = 0x00000000;
        private double viewRange = 64.0;
        private double shadowRadius = 0.0;
        private double shadowStrength = 1.0;

        public static TextDisplayOptions defaults() {
            return new TextDisplayOptions();
        }

        public boolean isBillboard() { return billboard; }
        public TextDisplayOptions setBillboard(boolean billboard) { this.billboard = billboard; return this; }

        public boolean isShadowed() { return shadowed; }
        public TextDisplayOptions setShadowed(boolean shadowed) { this.shadowed = shadowed; return this; }

        public boolean isSeeThrough() { return seeThrough; }
        public TextDisplayOptions setSeeThrough(boolean seeThrough) { this.seeThrough = seeThrough; return this; }

        public float getLineWidth() { return lineWidth; }
        public TextDisplayOptions setLineWidth(float lineWidth) { this.lineWidth = lineWidth; return this; }

        public int getBackgroundColor() { return backgroundColor; }
        public TextDisplayOptions setBackgroundColor(int backgroundColor) { this.backgroundColor = backgroundColor; return this; }
        public TextDisplayOptions setBackgroundColor(int r, int g, int b, int a) {
            this.backgroundColor = (a << 24) | (r << 16) | (g << 8) | b;
            return this;
        }

        public double getViewRange() { return viewRange; }
        public TextDisplayOptions setViewRange(double viewRange) { this.viewRange = viewRange; return this; }

        public double getShadowRadius() { return shadowRadius; }
        public TextDisplayOptions setShadowRadius(double shadowRadius) { this.shadowRadius = shadowRadius; return this; }

        public double getShadowStrength() { return shadowStrength; }
        public TextDisplayOptions setShadowStrength(double shadowStrength) { this.shadowStrength = shadowStrength; return this; }
    }
}
