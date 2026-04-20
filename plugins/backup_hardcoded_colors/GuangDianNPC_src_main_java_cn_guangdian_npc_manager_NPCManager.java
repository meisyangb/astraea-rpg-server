package cn.guangdian.npc.manager;

import cn.guangdian.npc.GuangDianNPC;
import cn.guangdian.npc.model.NPCData;
import cn.guangdian.npc.model.NPCType;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class NPCManager {

    private final GuangDianNPC plugin;
    private final Map<String, NPCData> npcs;
    private final Map<Integer, String> npcByEntityId;
    private final Map<UUID, String> npcByEntityUUID;
    private final Map<String, MenuDefinition> menus;

    private File npcFile;
    private FileConfiguration npcConfig;
    private AsyncExecutor asyncExecutor;
    private final Object saveLock = new Object();

    private static final String NPC_TAG = "guangdian_npc";
    private static final String NPC_ID_KEY = "npc_id";

    public NPCManager(GuangDianNPC plugin) {
        this.plugin = plugin;
        this.npcs = new ConcurrentHashMap<>();
        this.npcByEntityId = new ConcurrentHashMap<>();
        this.npcByEntityUUID = new ConcurrentHashMap<>();
        this.menus = new ConcurrentHashMap<>();
        this.npcFile = new File(plugin.getDataFolder(), "npcs.yml");
    }

    public void setAsyncExecutor(AsyncExecutor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
    }

    public void load() {
        loadNPCs();
        loadMenus();
    }

    private void loadNPCs() {
        npcs.clear();
        npcByEntityId.clear();
        npcByEntityUUID.clear();

        if (!npcFile.exists()) {
            plugin.saveResource("npcs.yml", false);
        }

        npcConfig = YamlConfiguration.loadConfiguration(npcFile);
        ConfigurationSection section = npcConfig.getConfigurationSection("npcs");
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection npcSection = section.getConfigurationSection(id);
            if (npcSection == null) {
                continue;
            }
            try {
                NPCData npc = NPCData.deserialize(id.toLowerCase(), npcSection);
                npcs.put(npc.getId(), npc);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "加载 NPC 失败: " + id, e);
            }
        }

        plugin.getLogger().info("已加载 " + npcs.size() + " 个 NPC");
    }

    private void loadMenus() {
        menus.clear();
        ConfigurationSection menusSection = plugin.getConfig().getConfigurationSection("menus");
        if (menusSection == null) {
            return;
        }

        for (String menuId : menusSection.getKeys(false)) {
            ConfigurationSection menuSection = menusSection.getConfigurationSection(menuId);
            if (menuSection == null) {
                continue;
            }
            MenuDefinition menu = new MenuDefinition(
                menuId.toLowerCase(),
                menuSection.getString("title", "&8NPC"),
                menuSection.getInt("size", 27)
            );

            ConfigurationSection itemsSection = menuSection.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String itemId : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
                    if (itemSection == null) {
                        continue;
                    }
                    menu.items.add(new MenuItemDefinition(
                        itemSection.getInt("slot", 0),
                        parseMaterial(itemSection.getString("material", "STONE")),
                        itemSection.getString("name", "&fItem"),
                        itemSection.getStringList("lore"),
                        itemSection.getString("action", "")
                    ));
                }
            }
            menus.put(menuId.toLowerCase(), menu);
        }

        plugin.getLogger().info("已加载 " + menus.size() + " 个菜单");
    }

    public void save() {
        if (asyncExecutor != null) {
            asyncExecutor.execute(this::saveInternal);
        } else {
            saveInternal();
        }
    }

    private void saveInternal() {
        synchronized (saveLock) {
            try {
                npcConfig.set("npcs", null);
                for (NPCData npc : npcs.values()) {
                    String path = "npcs." + npc.getId();
                    Map<String, Object> data = npc.serialize();
                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        npcConfig.set(path + "." + entry.getKey(), entry.getValue());
                    }
                }
                npcConfig.save(npcFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "保存 NPC 数据失败", e);
            }
        }
    }

    public void spawnAll() {
        cleanupResidualEntities();
        for (NPCData npc : npcs.values()) {
            if (npc.isEnabled()) {
                spawnNPC(npc);
            }
        }
    }

    public void despawnAll() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Villager villager && isManagedNPC(villager)) {
                    entity.remove();
                }
            }
        }
        npcByEntityId.clear();
        npcByEntityUUID.clear();
    }

    private void cleanupResidualEntities() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Villager villager) {
                    if (entity.getScoreboardTags().contains(NPC_TAG)) {
                        String npcId = entity.getPersistentDataContainer().get(
                            new org.bukkit.NamespacedKey(plugin, NPC_ID_KEY),
                            PersistentDataType.STRING
                        );
                        if (npcId == null || !npcs.containsKey(npcId)) {
                            entity.remove();
                            removed++;
                        }
                    }
                }
            }
        }
        if (removed > 0) {
            plugin.getLogger().info("已清理 " + removed + " 个残留 NPC 实体");
        }
    }

    public NPCData createNPC(String id, Player creator, String menuId) {
        String lowerId = id.toLowerCase();
        if (npcs.containsKey(lowerId)) {
            return null;
        }

        Location loc = creator.getLocation();
        NPCData npc = new NPCData(lowerId, "&e" + id, loc, menuId != null ? menuId.toLowerCase() : "main");
        npcs.put(lowerId, npc);
        spawnNPC(npc);
        save();

        return npc;
    }

    public boolean removeNPC(String id) {
        String lowerId = id.toLowerCase();
        NPCData npc = npcs.remove(lowerId);
        if (npc == null) {
            return false;
        }

        despawnNPC(npc);
        save();
        return true;
    }

    public void spawnNPC(NPCData npc) {
        World world = Bukkit.getWorld(npc.getWorldName());
        if (world == null) {
            plugin.getLogger().warning("NPC 世界不存在: " + npc.getId() + " -> " + npc.getWorldName());
            return;
        }

        Location location = new Location(world, npc.getX(), npc.getY(), npc.getZ(), npc.getYaw(), npc.getPitch());

        Villager villager = world.spawn(location, Villager.class, entity -> {
            entity.setAI(false);
            entity.setAware(false);
            entity.setInvulnerable(true);
            entity.setCollidable(false);
            entity.setCanPickupItems(false);
            entity.setPersistent(true);
            entity.setRemoveWhenFarAway(false);
            entity.setSilent(true);
            entity.setGravity(false);
            entity.setGlowing(false);
            
            String fullDisplayName = npc.getFullDisplayName();
            entity.setCustomNameVisible(true);
            entity.customName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacyAmpersand().deserialize(fullDisplayName));
            
            entity.setVillagerType(Villager.Type.PLAINS);
            entity.setProfession(Villager.Profession.NONE);
            entity.addScoreboardTag(NPC_TAG);
            entity.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, NPC_ID_KEY),
                PersistentDataType.STRING,
                npc.getId()
            );
        });

        npc.setEntityId(villager.getEntityId());
        npc.setEntityUUID(villager.getUniqueId());
        npcByEntityId.put(villager.getEntityId(), npc.getId());
        npcByEntityUUID.put(villager.getUniqueId(), npc.getId());
    }

    public void despawnNPC(NPCData npc) {
        if (npc.getEntityUUID() != null) {
            for (World world : Bukkit.getWorlds()) {
                Entity entity = world.getEntity(npc.getEntityUUID());
                if (entity != null) {
                    entity.remove();
                    break;
                }
            }
        }
        npcByEntityId.remove(npc.getEntityId());
        npcByEntityUUID.remove(npc.getEntityUUID());
        npc.setEntityId(0);
        npc.setEntityUUID(null);
    }

    public void respawnNPC(NPCData npc) {
        despawnNPC(npc);
        spawnNPC(npc);
    }

    public boolean isManagedNPC(Entity entity) {
        if (!(entity instanceof Villager)) {
            return false;
        }
        return entity.getScoreboardTags().contains(NPC_TAG) ||
               entity.getPersistentDataContainer().has(
                   new org.bukkit.NamespacedKey(plugin, NPC_ID_KEY),
                   PersistentDataType.STRING
               );
    }

    public NPCData getNPC(String id) {
        return npcs.get(id.toLowerCase());
    }

    public NPCData getNPCByEntityId(int entityId) {
        return npcs.get(npcByEntityId.get(entityId));
    }

    public NPCData getNPCByEntityUUID(UUID uuid) {
        return npcs.get(npcByEntityUUID.get(uuid));
    }

    public Map<String, NPCData> getNPCs() {
        return npcs;
    }

    public Collection<NPCData> getAllNPCs() {
        return Collections.unmodifiableCollection(npcs.values());
    }

    public int getNPCCount() {
        return npcs.size();
    }

    public MenuDefinition getMenu(String menuId) {
        return menus.get(menuId.toLowerCase());
    }

    public Collection<MenuDefinition> getAllMenus() {
        return Collections.unmodifiableCollection(menus.values());
    }

    public void reload() {
        despawnAll();
        load();
        spawnAll();
    }

    private Material parseMaterial(String input) {
        try {
            return Material.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }

    public static class MenuDefinition {
        private final String id;
        private final String title;
        private final int size;
        private final List<MenuItemDefinition> items;

        public MenuDefinition(String id, String title, int size) {
            this.id = id;
            this.title = title;
            this.size = size;
            this.items = new ArrayList<>();
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public int getSize() { return size; }
        public List<MenuItemDefinition> getItems() { return items; }
    }

    public static class MenuItemDefinition {
        private final int slot;
        private final Material material;
        private final String name;
        private final List<String> lore;
        private final String action;

        public MenuItemDefinition(int slot, Material material, String name, List<String> lore, String action) {
            this.slot = slot;
            this.material = material;
            this.name = name;
            this.lore = lore;
            this.action = action;
        }

        public int getSlot() { return slot; }
        public Material getMaterial() { return material; }
        public String getName() { return name; }
        public List<String> getLore() { return lore; }
        public String getAction() { return action; }
    }
}
