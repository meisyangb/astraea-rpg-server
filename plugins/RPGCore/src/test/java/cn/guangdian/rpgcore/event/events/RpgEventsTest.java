package cn.guangdian.rpgcore.event.events;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 公共事件测试
 * 
 * @author GuangDian
 * @since 1.0.0
 */
@DisplayName("RPG公共事件测试")
@ExtendWith(MockitoExtension.class)
class RpgEventsTest {

    @Mock
    private Player mockPlayer;

    @Mock
    private Entity mockEntity;

    @Test
    @DisplayName("RpgLevelUpEvent - 基本属性测试")
    void testLevelUpEventProperties() {
        when(mockPlayer.getName()).thenReturn("TestPlayer");
        
        RpgLevelUpEvent event = new RpgLevelUpEvent(mockPlayer, 10, 11, "EXP_GAIN");
        
        assertEquals(mockPlayer, event.getPlayer());
        assertEquals(10, event.getOldLevel());
        assertEquals(11, event.getNewLevel());
        assertEquals("EXP_GAIN", event.getSource());
        assertNotNull(event.getHandlers());
    }

    @Test
    @DisplayName("RpgLevelUpEvent - 默认来源测试")
    void testLevelUpEventDefaultSource() {
        RpgLevelUpEvent event = new RpgLevelUpEvent(mockPlayer, 5, 6);
        
        assertEquals("UNKNOWN", event.getSource());
    }

    @Test
    @DisplayName("RpgMobKillEvent - 普通怪物测试")
    void testMobKillEventNormalMob() {
        RpgMobKillEvent event = new RpgMobKillEvent(mockPlayer, mockEntity);
        
        assertEquals(mockPlayer, event.getKiller());
        assertEquals(mockEntity, event.getMob());
        assertFalse(event.isMythicMob());
        assertNull(event.getMythicMobType());
    }

    @Test
    @DisplayName("RpgMobKillEvent - MythicMobs怪物测试")
    void testMobKillEventMythicMob() {
        RpgMobKillEvent event = new RpgMobKillEvent(mockPlayer, mockEntity, true, "SKELETON_KING");
        
        assertTrue(event.isMythicMob());
        assertEquals("SKELETON_KING", event.getMythicMobType());
    }

    @Test
    @DisplayName("RpgMobKillEvent - 奖励设置测试")
    void testMobKillEventRewards() {
        RpgMobKillEvent event = new RpgMobKillEvent(mockPlayer, mockEntity);
        
        event.setExpReward(100);
        event.setMoneyReward(50.5);
        
        assertEquals(100, event.getExpReward());
        assertEquals(50.5, event.getMoneyReward());
    }

    @Test
    @DisplayName("RpgEconomyTransactionEvent - 存款测试")
    void testEconomyDepositEvent() {
        RpgEconomyTransactionEvent event = new RpgEconomyTransactionEvent(
            mockPlayer,
            RpgEconomyTransactionEvent.TransactionType.DEPOSIT,
            RpgEconomyTransactionEvent.CurrencyType.POINTS,
            100.0,
            0.0,
            100.0,
            "ADMIN_GIFT"
        );
        
        assertEquals(mockPlayer, event.getPlayer());
        assertEquals(RpgEconomyTransactionEvent.TransactionType.DEPOSIT, event.getTransactionType());
        assertEquals(RpgEconomyTransactionEvent.CurrencyType.POINTS, event.getCurrencyType());
        assertEquals(100.0, event.getAmount());
        assertEquals(0.0, event.getBalanceBefore());
        assertEquals(100.0, event.getBalanceAfter());
        assertEquals("ADMIN_GIFT", event.getReason());
    }

    @Test
    @DisplayName("RpgEconomyTransactionEvent - 取款测试")
    void testEconomyWithdrawEvent() {
        RpgEconomyTransactionEvent event = new RpgEconomyTransactionEvent(
            mockPlayer,
            RpgEconomyTransactionEvent.TransactionType.WITHDRAW,
            RpgEconomyTransactionEvent.CurrencyType.MONEY,
            500.0,
            1000.0,
            500.0,
            "SHOP_PURCHASE"
        );
        
        assertEquals(RpgEconomyTransactionEvent.TransactionType.WITHDRAW, event.getTransactionType());
        assertEquals(RpgEconomyTransactionEvent.CurrencyType.MONEY, event.getCurrencyType());
    }

    @Test
    @DisplayName("RpgSkillCastEvent - 技能释放测试")
    void testSkillCastEvent() {
        RpgSkillCastEvent event = new RpgSkillCastEvent(mockPlayer, "FIREBALL", "火球术", 50, 100);
        
        assertEquals(mockPlayer, event.getCaster());
        assertEquals("FIREBALL", event.getSkillId());
        assertEquals("火球术", event.getSkillName());
        assertEquals(50, event.getManaCost());
        assertEquals(100, event.getCooldownTicks());
        assertFalse(event.isCancelled());
    }

    @Test
    @DisplayName("RpgSkillCastEvent - 取消技能测试")
    void testSkillCastEventCancellation() {
        RpgSkillCastEvent event = new RpgSkillCastEvent(mockPlayer, "LIGHTNING", "闪电术", 80, 200);
        
        event.setCancelled(true);
        
        assertTrue(event.isCancelled());
    }

    @Test
    @DisplayName("RpgStatChangeEvent - 属性变更测试")
    void testStatChangeEvent() {
        RpgStatChangeEvent event = new RpgStatChangeEvent(mockPlayer, "ATTACK", 100.0, 150.0, "EQUIPMENT");
        
        assertEquals(mockPlayer, event.getPlayer());
        assertEquals("ATTACK", event.getStatType());
        assertEquals(100.0, event.getOldValue());
        assertEquals(150.0, event.getNewValue());
        assertEquals(50.0, event.getDifference());
        assertEquals("EQUIPMENT", event.getSource());
    }

    @Test
    @DisplayName("RpgStatChangeEvent - 默认来源测试")
    void testStatChangeEventDefaultSource() {
        RpgStatChangeEvent event = new RpgStatChangeEvent(mockPlayer, "DEFENSE", 50.0, 75.0);
        
        assertEquals("UNKNOWN", event.getSource());
    }

    @Test
    @DisplayName("RpgGuildEvent.Create - 公会创建测试")
    void testGuildCreateEvent() {
        when(mockPlayer.getName()).thenReturn("GuildMaster");
        
        RpgGuildEvent.Create event = new RpgGuildEvent.Create("guild-001", "TestGuild", mockPlayer);
        
        assertEquals("guild-001", event.getGuildId());
        assertEquals("TestGuild", event.getGuildName());
        assertEquals(mockPlayer, event.getCreator());
    }

    @Test
    @DisplayName("RpgGuildEvent.Join - 玩家加入公会测试")
    void testGuildJoinEvent() {
        RpgGuildEvent.Join event = new RpgGuildEvent.Join("guild-001", "TestGuild", mockPlayer);
        
        assertEquals(mockPlayer, event.getPlayer());
        assertEquals("guild-001", event.getGuildId());
    }

    @Test
    @DisplayName("RpgGuildEvent.Leave - 玩家退出公会测试")
    void testGuildLeaveEvent() {
        RpgGuildEvent.Leave event = new RpgGuildEvent.Leave(
            "guild-001", "TestGuild", mockPlayer, RpgGuildEvent.Leave.LeaveReason.QUIT
        );
        
        assertEquals(mockPlayer, event.getPlayer());
        assertEquals(RpgGuildEvent.Leave.LeaveReason.QUIT, event.getReason());
    }

    @Test
    @DisplayName("RpgGuildEvent.Leave - 被踢出公会测试")
    void testGuildKickEvent() {
        RpgGuildEvent.Leave event = new RpgGuildEvent.Leave(
            "guild-001", "TestGuild", mockPlayer, RpgGuildEvent.Leave.LeaveReason.KICK
        );
        
        assertEquals(RpgGuildEvent.Leave.LeaveReason.KICK, event.getReason());
    }

    @Test
    @DisplayName("RpgGuildEvent.Disband - 公会解散测试")
    void testGuildDisbandEvent() {
        RpgGuildEvent.Disband event = new RpgGuildEvent.Disband("guild-001", "TestGuild", mockPlayer);
        
        assertEquals("guild-001", event.getGuildId());
        assertEquals(mockPlayer, event.getDisbander());
    }
}
