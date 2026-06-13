package cn.guangdian.holo.api;

import cn.guangdian.holo.manager.HologramManager;
import cn.guangdian.holo.model.Hologram;
import org.bukkit.Location;

import java.util.Collection;
import java.util.List;

public class HologramAPIImpl implements HologramAPI {

    private final HologramManager hologramManager;

    public HologramAPIImpl(HologramManager hologramManager) {
        this.hologramManager = hologramManager;
    }

    @Override
    public Hologram getHologram(String name) {
        return hologramManager.getHologram(name);
    }

    @Override
    public Collection<Hologram> getAllHolograms() {
        return hologramManager.getAllHolograms();
    }

    @Override
    public List<String> getHologramNames() {
        return hologramManager.getHologramNames();
    }

    @Override
    public int getHologramCount() {
        return hologramManager.getHologramCount();
    }

    @Override
    public boolean hologramExists(String name) {
        return hologramManager.getHologram(name) != null;
    }

    @Override
    public Hologram createHologram(String name, Location location) {
        return hologramManager.createHologram(name, location);
    }

    @Override
    public boolean deleteHologram(String name) {
        return hologramManager.deleteHologram(name);
    }

    @Override
    public void addLine(String holoName, String text) {
        Hologram holo = hologramManager.getHologram(holoName);
        if (holo != null) {
            holo.addLine(text);
            hologramManager.respawnHologram(holo);
        }
    }

    @Override
    public void setLine(String holoName, int lineIndex, String text) {
        Hologram holo = hologramManager.getHologram(holoName);
        if (holo != null) {
            holo.setLine(lineIndex, text);
        }
    }

    @Override
    public void removeLine(String holoName, int lineIndex) {
        Hologram holo = hologramManager.getHologram(holoName);
        if (holo != null) {
            holo.removeLine(lineIndex);
            hologramManager.respawnHologram(holo);
        }
    }

    @Override
    public void clearLines(String holoName) {
        Hologram holo = hologramManager.getHologram(holoName);
        if (holo != null) {
            holo.clearLines();
            hologramManager.respawnHologram(holo);
        }
    }

    @Override
    public void teleport(String holoName, Location location) {
        Hologram holo = hologramManager.getHologram(holoName);
        if (holo != null) {
            holo.setLocation(location);
            hologramManager.respawnHologram(holo);
        }
    }

    @Override
    public void setViewDistance(String holoName, int distance) {
        Hologram holo = hologramManager.getHologram(holoName);
        if (holo != null) {
            holo.setViewDistance(distance);
        }
    }

    @Override
    public void setVisible(String holoName, boolean visible) {
        Hologram holo = hologramManager.getHologram(holoName);
        if (holo != null) {
            holo.setVisible(visible);
            if (visible) {
                hologramManager.spawnHologram(holo);
            } else {
                hologramManager.despawnHologram(holo);
            }
        }
    }
}
