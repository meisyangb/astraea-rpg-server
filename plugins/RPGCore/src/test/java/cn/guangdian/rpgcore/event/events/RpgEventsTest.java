package cn.guangdian.rpgcore.event.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 公共事件测试
 *
 * @author GuangDian
 * @since 1.0.0
 */
@DisplayName("RPG公共事件测试")
class RpgEventsTest {

    @Test
    @DisplayName("测试事件类存在")
    void testEventClassesExist() {
        // 验证事件类可以被加载
        assertDoesNotThrow(() -> {
            Class.forName("cn.guangdian.rpgcore.event.events.RpgLevelUpEvent");
            Class.forName("cn.guangdian.rpgcore.event.events.RpgMobKillEvent");
            Class.forName("cn.guangdian.rpgcore.event.events.RpgEconomyTransactionEvent");
            Class.forName("cn.guangdian.rpgcore.event.events.RpgSkillCastEvent");
            Class.forName("cn.guangdian.rpgcore.event.events.RpgStatChangeEvent");
            Class.forName("cn.guangdian.rpgcore.event.events.RpgGuildEvent");
        });
    }

    @Test
    @DisplayName("测试枚举类型")
    void testEnumTypes() {
        // 验证TransactionType枚举
        assertDoesNotThrow(() -> {
            RpgEconomyTransactionEvent.TransactionType[] types = RpgEconomyTransactionEvent.TransactionType.values();
            assertTrue(types.length > 0);
        });

        // 验证CurrencyType枚举
        assertDoesNotThrow(() -> {
            RpgEconomyTransactionEvent.CurrencyType[] currencies = RpgEconomyTransactionEvent.CurrencyType.values();
            assertTrue(currencies.length > 0);
        });

        // 验证LeaveReason枚举
        assertDoesNotThrow(() -> {
            RpgGuildEvent.Leave.LeaveReason[] reasons = RpgGuildEvent.Leave.LeaveReason.values();
            assertTrue(reasons.length > 0);
        });
    }

    @Test
    @DisplayName("测试TransactionType枚举值")
    void testTransactionTypeValues() {
        RpgEconomyTransactionEvent.TransactionType[] types = RpgEconomyTransactionEvent.TransactionType.values();

        boolean hasDeposit = false;
        boolean hasWithdraw = false;

        for (RpgEconomyTransactionEvent.TransactionType type : types) {
            if (type.name().equals("DEPOSIT")) hasDeposit = true;
            if (type.name().equals("WITHDRAW")) hasWithdraw = true;
        }

        assertTrue(hasDeposit, "应该包含DEPOSIT类型");
        assertTrue(hasWithdraw, "应该包含WITHDRAW类型");
    }

    @Test
    @DisplayName("测试CurrencyType枚举值")
    void testCurrencyTypeValues() {
        RpgEconomyTransactionEvent.CurrencyType[] currencies = RpgEconomyTransactionEvent.CurrencyType.values();
        assertTrue(currencies.length >= 2, "应该至少包含两种货币类型");
    }

    @Test
    @DisplayName("测试LeaveReason枚举值")
    void testLeaveReasonValues() {
        RpgGuildEvent.Leave.LeaveReason[] reasons = RpgGuildEvent.Leave.LeaveReason.values();

        boolean hasQuit = false;
        boolean hasKick = false;

        for (RpgGuildEvent.Leave.LeaveReason reason : reasons) {
            if (reason.name().equals("QUIT")) hasQuit = true;
            if (reason.name().equals("KICK")) hasKick = true;
        }

        assertTrue(hasQuit, "应该包含QUIT原因");
        assertTrue(hasKick, "应该包含KICK原因");
    }

    @Test
    @DisplayName("测试事件类结构")
    void testEventClassStructure() {
        // 验证RpgLevelUpEvent有正确的构造函数
        assertDoesNotThrow(() -> {
            RpgLevelUpEvent.class.getDeclaredConstructor(
                org.bukkit.entity.Player.class, int.class, int.class, String.class
            );
        });

        // 验证RpgMobKillEvent有正确的构造函数
        assertDoesNotThrow(() -> {
            RpgMobKillEvent.class.getDeclaredConstructor(
                org.bukkit.entity.Player.class, org.bukkit.entity.Entity.class
            );
        });
    }

    @Test
    @DisplayName("测试事件类方法存在")
    void testEventMethodsExist() {
        // 验证RpgLevelUpEvent有getPlayer方法
        assertDoesNotThrow(() -> {
            RpgLevelUpEvent.class.getMethod("getPlayer");
        });

        // 验证RpgLevelUpEvent有getOldLevel方法
        assertDoesNotThrow(() -> {
            RpgLevelUpEvent.class.getMethod("getOldLevel");
        });

        // 验证RpgLevelUpEvent有getNewLevel方法
        assertDoesNotThrow(() -> {
            RpgLevelUpEvent.class.getMethod("getNewLevel");
        });

        // 验证RpgLevelUpEvent有getSource方法
        assertDoesNotThrow(() -> {
            RpgLevelUpEvent.class.getMethod("getSource");
        });
    }
}
