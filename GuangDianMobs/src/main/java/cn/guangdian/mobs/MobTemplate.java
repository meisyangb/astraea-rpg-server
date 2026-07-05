package cn.guangdian.mobs;

import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * 怪物模板 — MM 风格，支持内联技能 + 指令掉落
 * <p>技能格式：type{key=val;...} @Target ~onTrigger:interval [chance]</p>
 */
public record MobTemplate(
    String id,
    String displayName,
    EntityType entityType,
    int level,
    double health, double damage, double defense,
    double speed, double attackSpeed, double followRange, double knockbackResist,
    Map<String, ItemStack> equipment,
    List<SkillLine> skills,         // 内联技能行
    Map<String, Double> damageMods,
    Opts opts,
    BossBar bossBar
) {

    // ════════════════════════════════════════
    //  内联技能行: "damage{amount=100} @Target ~onTimer:60"
    //  指令掉落:   "command{c=give ...} @Trigger ~onDeath 0.1"
    // ════════════════════════════════════════
    public record SkillLine(String raw) {
        /** 解析参数 {key=val;key=val} — 用括号计数找匹配的 } */
        public Map<String, String> params() {
            Map<String, String> m = new LinkedHashMap<>();
            int s = raw.indexOf('{');
            if (s < 0) return m;
            int depth = 0, e = s;
            while (e < raw.length()) {
                char c = raw.charAt(e);
                if (c == '{') depth++; else if (c == '}') depth--;
                if (depth == 0) break;
                e++;
            }
            if (e >= raw.length()) return m;
            for (String p : raw.substring(s + 1, e).split(";")) {
                int eq = p.indexOf('=');
                if (eq > 0) m.put(p.substring(0, eq).trim(), p.substring(eq + 1).trim());
            }
            return m;
        }
        /** 技能类型名 (damage / command / potion / skill / message / heal) */
        public String type() {
            int b = raw.indexOf('{');
            return b > 0 ? raw.substring(0, b).trim().toLowerCase() : raw.split(" ")[0].toLowerCase();
        }
        /** 目标选择器 @Target / @Self / @PlayersInRadius{r=5} / @Trigger */
        public String target() {
            for (String p : raw.split(" ")) if (p.startsWith("@")) return p;
            return "@Self";
        }
        /** 触发器 ~onTimer:60 / ~onDeath / ~onSpawn / ~onHit */
        public String trigger() { return raw.contains("~onTimer") ? "timer" : raw.contains("~onDeath") ? "death" : raw.contains("~onSpawn") ? "spawn" : raw.contains("~onHit") ? "hit" : "timer"; }
        /** timerr间隔(tick) */
        public int interval() {
            int i = raw.indexOf("~onTimer:");
            if (i < 0) return 100;
            int end = raw.indexOf(' ', i);
            if (end < 0) end = raw.length();
            try { return Integer.parseInt(raw.substring(i + 9, end).trim()); }
            catch (Exception e) { return 100; }
        }
        /** 概率 (行末数字或 1.0) */
        public double chance() {
            String[] parts = raw.split(" ");
            try { return Double.parseDouble(parts[parts.length - 1]); }
            catch (Exception e) { return 1.0; }
        }
        @Override public String toString() { return raw; }
    }

    // ════════════════════════════════════════
    public record Opts(boolean showName, boolean preventDrops, int size) {}

    public record BossBar(boolean enabled, String color, String style, String title) {
        public static BossBar NONE = new BossBar(false, "RED", "SOLID", "");
    }

    public boolean isValid() { return id != null && !id.isBlank() && entityType != null; }
}
