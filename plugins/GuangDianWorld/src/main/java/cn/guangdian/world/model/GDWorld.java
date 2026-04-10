package cn.guangdian.world.model;

import org.bukkit.Location;
import org.bukkit.World;

public class GDWorld {

    private final String name;
    private String alias;
    private World.Environment environment;
    private String difficulty;
    private String gamemode;
    private boolean pvp;
    private boolean allowFlight;
    private boolean allowWeather;
    private boolean hunger;
    private boolean keepSpawnInMemory;
    private boolean autoLoad;
    private boolean doMobSpawning;
    private boolean doFireTick;
    private boolean keepInventory;
    private String respawnWorld;
    private String generator;
    private Location spawnLocation;
    private World bukkitWorld;

    public GDWorld(String name) {
        this.name = name;
        this.environment = World.Environment.NORMAL;
        this.difficulty = "normal";
        this.gamemode = "survival";
        this.pvp = true;
        this.allowFlight = false;
        this.allowWeather = true;
        this.hunger = true;
        this.keepSpawnInMemory = false;
        this.autoLoad = true;
        this.doMobSpawning = true;
        this.doFireTick = true;
        this.keepInventory = false;
        this.generator = "";
    }

    public String getName() {
        return name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getDisplayName() {
        return (alias != null && !alias.isEmpty()) ? alias : name;
    }

    public World.Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(World.Environment environment) {
        this.environment = environment;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getGamemode() {
        return gamemode;
    }

    public void setGamemode(String gamemode) {
        this.gamemode = gamemode;
    }

    public boolean isPvp() {
        return pvp;
    }

    public void setPvp(boolean pvp) {
        this.pvp = pvp;
    }

    public boolean isAllowFlight() {
        return allowFlight;
    }

    public void setAllowFlight(boolean allowFlight) {
        this.allowFlight = allowFlight;
    }

    public boolean isAllowWeather() {
        return allowWeather;
    }

    public void setAllowWeather(boolean allowWeather) {
        this.allowWeather = allowWeather;
    }

    public boolean isHunger() {
        return hunger;
    }

    public void setHunger(boolean hunger) {
        this.hunger = hunger;
    }

    public boolean isKeepSpawnInMemory() {
        return keepSpawnInMemory;
    }

    public void setKeepSpawnInMemory(boolean keepSpawnInMemory) {
        this.keepSpawnInMemory = keepSpawnInMemory;
    }

    public boolean isAutoLoad() {
        return autoLoad;
    }

    public void setAutoLoad(boolean autoLoad) {
        this.autoLoad = autoLoad;
    }

    public boolean isDoMobSpawning() {
        return doMobSpawning;
    }

    public void setDoMobSpawning(boolean doMobSpawning) {
        this.doMobSpawning = doMobSpawning;
    }

    public boolean isDoFireTick() {
        return doFireTick;
    }

    public void setDoFireTick(boolean doFireTick) {
        this.doFireTick = doFireTick;
    }

    public boolean isKeepInventory() {
        return keepInventory;
    }

    public void setKeepInventory(boolean keepInventory) {
        this.keepInventory = keepInventory;
    }

    public String getRespawnWorld() {
        return respawnWorld;
    }

    public void setRespawnWorld(String respawnWorld) {
        this.respawnWorld = respawnWorld;
    }

    public String getGenerator() {
        return generator;
    }

    public void setGenerator(String generator) {
        this.generator = generator;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public World getBukkitWorld() {
        return bukkitWorld;
    }

    public void setBukkitWorld(World bukkitWorld) {
        this.bukkitWorld = bukkitWorld;
    }

    public boolean isLoaded() {
        return bukkitWorld != null;
    }
}
