package cn.guangdian.holo.api;

import cn.guangdian.holo.model.Hologram;
import org.bukkit.Location;

import java.util.Collection;
import java.util.List;

public interface HologramAPI {

    Hologram getHologram(String name);

    Collection<Hologram> getAllHolograms();

    List<String> getHologramNames();

    int getHologramCount();

    boolean hologramExists(String name);

    Hologram createHologram(String name, Location location);

    boolean deleteHologram(String name);

    void addLine(String holoName, String text);

    void setLine(String holoName, int lineIndex, String text);

    void removeLine(String holoName, int lineIndex);

    void clearLines(String holoName);

    void teleport(String holoName, Location location);

    void setViewDistance(String holoName, int distance);

    void setVisible(String holoName, boolean visible);
}
