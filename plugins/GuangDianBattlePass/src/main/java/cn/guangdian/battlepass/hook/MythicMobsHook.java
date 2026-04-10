package cn.guangdian.battlepass.hook;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Method;

public class MythicMobsHook {
    
    private static boolean mythicMobsEnabled = false;
    private static Object mythicBukkitInstance;
    private static Object mobManager;
    private static Method getActiveMobMethod;
    private static Method getMobTypeMethod;
    private static final NamespacedKey MYTHIC_TYPE_KEY = new NamespacedKey("mythicmobs", "type");
    
    public static void checkMythicMobs() {
        try {
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Method instMethod = mythicBukkitClass.getMethod("inst");
            mythicBukkitInstance = instMethod.invoke(null);
            
            Method getMobManagerMethod = mythicBukkitClass.getMethod("getMobManager");
            mobManager = getMobManagerMethod.invoke(mythicBukkitInstance);
            
            Class<?> mobManagerClass = mobManager.getClass();
            getActiveMobMethod = mobManagerClass.getMethod("getActiveMob", java.util.UUID.class);
            
            Class<?> activeMobClass = Class.forName("io.lumine.mythic.core.mobs.ActiveMob");
            getMobTypeMethod = activeMobClass.getMethod("getMobType");
            
            mythicMobsEnabled = true;
        } catch (Exception e) {
            mythicMobsEnabled = false;
        }
    }
    
    public static boolean isMythicMobsEnabled() {
        return mythicMobsEnabled;
    }
    
    public static String getMythicMobType(Entity entity) {
        if (!mythicMobsEnabled || entity == null) {
            return null;
        }
        
        try {
            if (entity instanceof LivingEntity) {
                PersistentDataContainer pdc = ((LivingEntity) entity).getPersistentDataContainer();
                if (pdc.has(MYTHIC_TYPE_KEY, PersistentDataType.STRING)) {
                    return pdc.get(MYTHIC_TYPE_KEY, PersistentDataType.STRING);
                }
                
                Object activeMobOpt = getActiveMobMethod.invoke(mobManager, entity.getUniqueId());
                if (activeMobOpt != null) {
                    Method isPresentMethod = activeMobOpt.getClass().getMethod("isPresent");
                    boolean isPresent = (boolean) isPresentMethod.invoke(activeMobOpt);
                    
                    if (isPresent) {
                        Method getMethod = activeMobOpt.getClass().getMethod("get");
                        Object activeMob = getMethod.invoke(activeMobOpt);
                        
                        if (activeMob != null) {
                            return (String) getMobTypeMethod.invoke(activeMob);
                        }
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        
        return null;
    }
    
    public static boolean isMythicMob(Entity entity) {
        return getMythicMobType(entity) != null;
    }
    
    public static String getMythicItemType(ItemStack item) {
        if (!mythicMobsEnabled || item == null || !item.hasItemMeta()) {
            return null;
        }
        
        try {
            ItemMeta meta = item.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            
            if (pdc.has(MYTHIC_TYPE_KEY, PersistentDataType.STRING)) {
                return pdc.get(MYTHIC_TYPE_KEY, PersistentDataType.STRING);
            }
        } catch (Exception e) {
            return null;
        }
        
        return null;
    }
    
    public static boolean isMythicItem(ItemStack item) {
        return getMythicItemType(item) != null;
    }
}
