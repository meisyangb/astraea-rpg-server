package cn.guangdian.rpgcore.event.events;

import cn.guangdian.rpgcore.event.events.skill.RpgSkillCastEvent;
import cn.guangdian.rpgcore.event.events.skill.RpgSkillCooldownEvent;
import cn.guangdian.rpgcore.event.events.skill.RpgSkillDamageEvent;
import cn.guangdian.rpgcore.event.events.skill.RpgSkillLearnEvent;
import cn.guangdian.rpgcore.event.events.skill.RpgSkillPointEvent;
import cn.guangdian.rpgcore.event.events.skill.RpgSkillUpgradeEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RPGCore 事件测试
 *
 * @author GuangDian
 * @since 2.0.0
 */
@DisplayName("RPGCore 事件测试")
class RpgEventsTest {

    @Test
    @DisplayName("测试核心事件类存在")
    void testCoreEventClassesExist() {
        assertDoesNotThrow(() -> {
            Class.forName("cn.guangdian.rpgcore.event.events.ModuleEnableEvent");
            Class.forName("cn.guangdian.rpgcore.event.events.PlayerDataLoadEvent");
            Class.forName("cn.guangdian.rpgcore.event.events.PlayerDataSaveEvent");
        });
    }

    @Test
    @DisplayName("测试技能事件类存在")
    void testSkillEventClassesExist() {
        assertDoesNotThrow(() -> {
            Class.forName("cn.guangdian.rpgcore.event.events.skill.RpgSkillCastEvent");
            Class.forName("cn.guangdian.rpgcore.event.events.skill.RpgSkillCooldownEvent");
            Class.forName("cn.guangdian.rpgcore.event.events.skill.RpgSkillDamageEvent");
            Class.forName("cn.guangdian.rpgcore.event.events.skill.RpgSkillLearnEvent");
            Class.forName("cn.guangdian.rpgcore.event.events.skill.RpgSkillPointEvent");
            Class.forName("cn.guangdian.rpgcore.event.events.skill.RpgSkillUpgradeEvent");
        });
    }

    @Test
    @DisplayName("测试 PlayerDataLoadEvent 构造")
    void testPlayerDataLoadEventConstruction() {
        UUID playerId = UUID.randomUUID();
        String module = "Points";
        Object data = new Object();
        
        PlayerDataLoadEvent event = new PlayerDataLoadEvent(playerId, module, data);
        
        assertNotNull(event);
        assertEquals(playerId, event.getPlayerId());
        assertEquals(module, event.getModule());
        assertEquals(data, event.getData());
    }

    @Test
    @DisplayName("测试 PlayerDataSaveEvent 构造")
    void testPlayerDataSaveEventConstruction() {
        UUID playerId = UUID.randomUUID();
        String module = "Points";
        Object data = new Object();
        
        PlayerDataSaveEvent event = new PlayerDataSaveEvent(playerId, module, data);
        
        assertNotNull(event);
        assertEquals(playerId, event.getPlayerId());
        assertEquals(module, event.getModule());
        assertEquals(data, event.getData());
    }

    @Test
    @DisplayName("测试 ModuleEnableEvent 构造")
    void testModuleEnableEventConstruction() {
        String moduleId = "TestModule";
        String version = "1.0.0";
        
        ModuleEnableEvent event = new ModuleEnableEvent(moduleId, version);
        
        assertNotNull(event);
        assertEquals(moduleId, event.getModuleId());
        assertEquals(version, event.getVersion());
    }

    @Test
    @DisplayName("测试事件继承 Bukkit Event")
    void testEventsExtendBukkitEvent() {
        assertTrue(org.bukkit.event.Event.class.isAssignableFrom(PlayerDataLoadEvent.class));
        assertTrue(org.bukkit.event.Event.class.isAssignableFrom(PlayerDataSaveEvent.class));
        assertTrue(org.bukkit.event.Event.class.isAssignableFrom(ModuleEnableEvent.class));
        assertTrue(org.bukkit.event.Event.class.isAssignableFrom(RpgSkillCastEvent.class));
    }

    @Test
    @DisplayName("测试事件有 HandlerList")
    void testEventsHaveHandlerList() throws NoSuchMethodException {
        assertNotNull(PlayerDataLoadEvent.class.getMethod("getHandlerList"));
        assertNotNull(PlayerDataSaveEvent.class.getMethod("getHandlerList"));
        assertNotNull(ModuleEnableEvent.class.getMethod("getHandlerList"));
        assertNotNull(RpgSkillCastEvent.class.getMethod("getHandlerList"));
    }

    @Test
    @DisplayName("测试事件可取消性")
    void testEventCancellable() {
        PlayerDataLoadEvent loadEvent = new PlayerDataLoadEvent(
            UUID.randomUUID(), "Test", new Object()
        );
        
        assertFalse(loadEvent.isCancelled());
        loadEvent.setCancelled(true);
        assertTrue(loadEvent.isCancelled());
    }
}
