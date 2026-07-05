package cn.guangdian.classsystem.enums;

/**
 * 技能枚举 - 克苏鲁风格技能定义
 * 直接枚举所有技能，不使用循环查找
 */
public enum Skill {
    
    // ========================================
    // 深渊途径技能
    // ========================================
    
    // 序列9 - 深渊行者
    ABYSS_SLASH("深渊斩击", "abyss_slash", SkillType.ACTIVE, Pathway.ABYSS, Sequence.SEQ_9, Branch.NONE, 1.5, 4.0, 5, 10, "darkness"),
    ABYSS_SENSE("深渊感知", "abyss_sense", SkillType.PASSIVE, Pathway.ABYSS, Sequence.SEQ_9, Branch.NONE, 0, 0, 0, 0, "sense"),
    
    // 序列8 - 深渊守卫
    DARK_SHIELD("黑暗护盾", "dark_shield", SkillType.ACTIVE, Pathway.ABYSS, Sequence.SEQ_8, Branch.NONE, 1.0, 0, 20, 15, "shield"),
    ABYSS_RESILIENCE("深渊坚韧", "abyss_resilience", SkillType.PASSIVE, Pathway.ABYSS, Sequence.SEQ_8, Branch.NONE, 0, 0, 0, 0, "health_boost"),
    
    // 序列7 - 深渊骑士
    ABYSS_CHARGE("深渊冲锋", "abyss_charge", SkillType.ACTIVE, Pathway.ABYSS, Sequence.SEQ_7, Branch.NONE, 3.0, 6.0, 15, 20, "charge"),
    ABYSS_POWER("深渊之力", "abyss_power", SkillType.PASSIVE, Pathway.ABYSS, Sequence.SEQ_7, Branch.NONE, 1.5, 0, 0, 0, "damage_boost"),
    
    // 序列7 - 狂战士分支
    ABYSS_FURY("深渊狂怒", "abyss_fury", SkillType.ACTIVE, Pathway.ABYSS, Sequence.SEQ_7, Branch.BERSERKER, 1.5, 0, 30, 25, "rage"),
    ABYSS_RAGE("深渊暴怒", "abyss_rage", SkillType.PASSIVE, Pathway.ABYSS, Sequence.SEQ_7, Branch.BERSERKER, 0, 0, 0, 0, "kill_boost"),
    
    // 序列7 - 守护者分支
    ABYSS_GUARD("深渊守护", "abyss_guard", SkillType.ACTIVE, Pathway.ABYSS, Sequence.SEQ_7, Branch.DEFENDER, 1.0, 5.0, 25, 30, "guard"),
    ABYSS_WALL("深渊壁垒", "abyss_wall", SkillType.PASSIVE, Pathway.ABYSS, Sequence.SEQ_7, Branch.DEFENDER, 0, 0, 0, 0, "defense_boost"),
    
    // 序列6 - 深渊领主
    ABYSS_SUMMON("深渊召唤", "abyss_summon", SkillType.ACTIVE, Pathway.ABYSS, Sequence.SEQ_6, Branch.NONE, 2.0, 8.0, 60, 50, "summon"),
    ABYSS_PRESSURE("深渊威压", "abyss_pressure", SkillType.PASSIVE, Pathway.ABYSS, Sequence.SEQ_6, Branch.NONE, 0, 8.0, 0, 0, "weaken"),
    
    // 序列5 - 深渊暴君
    ABYSS_JUDGMENT("深渊审判", "abyss_judgment", SkillType.ACTIVE, Pathway.ABYSS, Sequence.SEQ_5, Branch.NONE, 5.0, 4.0, 30, 40, "judgment"),
    ABYSS_DEVOUR("深渊吞噬", "abyss_devour", SkillType.PASSIVE, Pathway.ABYSS, Sequence.SEQ_5, Branch.NONE, 0, 0, 0, 0, "heal_on_kill"),
    
    // 序列4 - 深渊神选
    ABYSS_ANNIHILATE("深渊湮灭", "abyss_annihilate", SkillType.ACTIVE, Pathway.ABYSS, Sequence.SEQ_4, Branch.NONE, 8.0, 8.0, 60, 80, "annihilate"),
    ABYSS_AVATAR("深渊化身", "abyss_avatar", SkillType.PASSIVE, Pathway.ABYSS, Sequence.SEQ_4, Branch.NONE, 0, 0, 0, 0, "avatar"),
    
    // 序列3 - 深渊之王
    ABYSS_REALM("深渊领域", "abyss_realm", SkillType.ACTIVE, Pathway.ABYSS, Sequence.SEQ_3, Branch.NONE, 1.0, 15.0, 120, 100, "realm"),
    ABYSS_RULE("深渊统治", "abyss_rule", SkillType.PASSIVE, Pathway.ABYSS, Sequence.SEQ_3, Branch.NONE, 0, 15.0, 0, 0, "dominate"),
    
    // 序列2 - 深渊主宰
    ABYSS_WILL("深渊意志", "abyss_will", SkillType.ACTIVE, Pathway.ABYSS, Sequence.SEQ_2, Branch.NONE, 1.0, 20.0, 180, 150, "will"),
    ABYSS_IMMORTAL("深渊不死", "abyss_immortal", SkillType.PASSIVE, Pathway.ABYSS, Sequence.SEQ_2, Branch.NONE, 0, 0, 0, 0, "immortal"),
    
    // 序列1 - 深渊神子
    ABYSS_GOD_POWER("深渊神力", "abyss_god_power", SkillType.ACTIVE, Pathway.ABYSS, Sequence.SEQ_1, Branch.NONE, 2.0, 0, 300, 200, "god_power"),
    ABYSS_CREATE("深渊创造", "abyss_create", SkillType.PASSIVE, Pathway.ABYSS, Sequence.SEQ_1, Branch.NONE, 0, 0, 0, 0, "create"),
    
    // 序列0 - 深渊之主（真神）
    ABYSS_CREATION("深渊创造", "abyss_creation", SkillType.ACTIVE, Pathway.ABYSS, Sequence.SEQ_0, Branch.NONE, 100.0, 50.0, 600, 500, "creation"),
    ABYSS_DOMINATION("深渊主宰", "abyss_domination", SkillType.PASSIVE, Pathway.ABYSS, Sequence.SEQ_0, Branch.NONE, 0, 0, 0, 0, "domination"),
    
    // ========================================
    // 虚空途径技能
    // ========================================
    
    // 序列9 - 虚空学徒
    VOID_FIREBALL("虚空火球", "void_fireball", SkillType.ACTIVE, Pathway.VOID, Sequence.SEQ_9, Branch.NONE, 2.0, 6.0, 8, 15, "fire"),
    VOID_SENSE("虚空感知", "void_sense", SkillType.PASSIVE, Pathway.VOID, Sequence.SEQ_9, Branch.NONE, 0, 0, 0, 0, "sense"),
    
    // 序列8 - 虚空术士
    VOID_ICE("虚空冰霜", "void_ice", SkillType.ACTIVE, Pathway.VOID, Sequence.SEQ_8, Branch.NONE, 1.5, 5.0, 15, 20, "ice"),
    VOID_SHIELD("虚空护盾", "void_shield", SkillType.PASSIVE, Pathway.VOID, Sequence.SEQ_8, Branch.NONE, 0, 0, 0, 0, "shield"),
    
    // 序列7 - 虚空大师
    VOID_CONTROL("虚空操控", "void_control", SkillType.ACTIVE, Pathway.VOID, Sequence.SEQ_7, Branch.NONE, 3.0, 8.0, 20, 30, "magic"),
    VOID_ERODE("虚空侵蚀", "void_erode", SkillType.PASSIVE, Pathway.VOID, Sequence.SEQ_7, Branch.NONE, 0, 0, 0, 0, "defense_down"),
    
    // 序列7 - 毁灭者分支
    VOID_BURST("虚空爆发", "void_burst", SkillType.ACTIVE, Pathway.VOID, Sequence.SEQ_7, Branch.DESTROYER, 5.0, 6.0, 25, 40, "burst"),
    VOID_DESTROY("虚空毁灭", "void_destroy", SkillType.PASSIVE, Pathway.VOID, Sequence.SEQ_7, Branch.DESTROYER, 0, 0, 0, 0, "fear"),
    
    // 序列7 - 支配者分支
    VOID_BIND("虚空禁锢", "void_bind", SkillType.ACTIVE, Pathway.VOID, Sequence.SEQ_7, Branch.CONTROLLER, 1.0, 10.0, 20, 35, "bind"),
    VOID_DOMINATE("虚空支配", "void_dominate", SkillType.PASSIVE, Pathway.VOID, Sequence.SEQ_7, Branch.CONTROLLER, 0, 0, 0, 0, "dominate"),
    
    // 序列6 - 虚空领主
    VOID_DOMAIN("虚空领域", "void_domain", SkillType.ACTIVE, Pathway.VOID, Sequence.SEQ_6, Branch.NONE, 2.0, 10.0, 60, 50, "domain"),
    
    // 序列5 - 虚空毁灭者
    VOID_METEOR("虚空陨石", "void_meteor", SkillType.ACTIVE, Pathway.VOID, Sequence.SEQ_5, Branch.NONE, 8.0, 12.0, 45, 60, "meteor"),
    VOID_COLLAPSE("虚空崩溃", "void_collapse", SkillType.PASSIVE, Pathway.VOID, Sequence.SEQ_5, Branch.NONE, 0, 0, 0, 0, "fear"),
    
    // 序列4 - 虚空神选
    VOID_ANNIHILATE("虚空湮灭", "void_annihilate", SkillType.ACTIVE, Pathway.VOID, Sequence.SEQ_4, Branch.NONE, 10.0, 10.0, 60, 80, "annihilate"),
    VOID_AVATAR("虚空化身", "void_avatar", SkillType.PASSIVE, Pathway.VOID, Sequence.SEQ_4, Branch.NONE, 0, 0, 0, 0, "avatar"),
    
    // 序列3 - 虚空之王
    TIME_STOP("时间停止", "time_stop", SkillType.ACTIVE, Pathway.VOID, Sequence.SEQ_3, Branch.NONE, 1.0, 20.0, 120, 100, "time_stop"),
    VOID_ETERNAL("虚空永恒", "void_eternal", SkillType.PASSIVE, Pathway.VOID, Sequence.SEQ_3, Branch.NONE, 0, 0, 0, 0, "time_immunity"),
    
    // ========================================
    // 暗影途径技能
    // ========================================
    
    // 序列9 - 暗影行者
    SHADOW_STEALTH("暗影潜行", "shadow_stealth", SkillType.ACTIVE, Pathway.SHADOW, Sequence.SEQ_9, Branch.NONE, 0, 0, 10, 10, "stealth"),
    SHADOW_SENSE("暗影感知", "shadow_sense", SkillType.PASSIVE, Pathway.SHADOW, Sequence.SEQ_9, Branch.NONE, 0, 0, 0, 0, "sense"),
    
    // 序列8 - 暗影猎手
    SHADOW_BACKSTAB("暗影背刺", "shadow_backstab", SkillType.ACTIVE, Pathway.SHADOW, Sequence.SEQ_8, Branch.NONE, 2.5, 3.0, 8, 15, "backstab"),
    SHADOW_TRACK("暗影追踪", "shadow_track", SkillType.PASSIVE, Pathway.SHADOW, Sequence.SEQ_8, Branch.NONE, 0, 0, 0, 0, "track"),
    
    // 序列7 - 暗影刺客
    SHADOW_STORM("暗影风暴", "shadow_storm", SkillType.ACTIVE, Pathway.SHADOW, Sequence.SEQ_7, Branch.NONE, 4.0, 6.0, 15, 25, "storm"),
    SHADOW_MASTERY("暗影精通", "shadow_mastery", SkillType.PASSIVE, Pathway.SHADOW, Sequence.SEQ_7, Branch.NONE, 0, 0, 0, 0, "mastery"),
    
    // 序列7 - 杀手分支
    SHADOW_KILL("暗影杀戮", "shadow_kill", SkillType.ACTIVE, Pathway.SHADOW, Sequence.SEQ_7, Branch.KILLER, 6.0, 4.0, 20, 30, "kill"),
    SHADOW_MARK("暗影标记", "shadow_mark", SkillType.PASSIVE, Pathway.SHADOW, Sequence.SEQ_7, Branch.KILLER, 0, 0, 0, 0, "mark"),
    
    // 序列7 - 舞者分支
    SHADOW_DANCE("暗影狂舞", "shadow_dance", SkillType.ACTIVE, Pathway.SHADOW, Sequence.SEQ_7, Branch.DANCER, 3.0, 8.0, 18, 28, "dance"),
    SHADOW_WAVE("暗影波动", "shadow_wave", SkillType.PASSIVE, Pathway.SHADOW, Sequence.SEQ_7, Branch.DANCER, 0, 0, 0, 0, "wave"),
    
    // ========================================
    // 腐化途径技能
    // ========================================
    
    // 序列9 - 腐化行者
    CORRUPTION_HEAL("腐化治愈", "corruption_heal", SkillType.ACTIVE, Pathway.CORRUPTION, Sequence.SEQ_9, Branch.NONE, 0, 5.0, 10, 15, "heal"),
    CORRUPTION_SENSE("腐化感知", "corruption_sense", SkillType.PASSIVE, Pathway.CORRUPTION, Sequence.SEQ_9, Branch.NONE, 0, 0, 0, 0, "sense"),
    
    // 序列8 - 腐化祭司
    TABOO_BLESSING("禁忌祝福", "taboo_blessing", SkillType.ACTIVE, Pathway.CORRUPTION, Sequence.SEQ_8, Branch.NONE, 0, 6.0, 12, 20, "blessing"),
    CORRUPTION_SHIELD("腐化护盾", "corruption_shield", SkillType.PASSIVE, Pathway.CORRUPTION, Sequence.SEQ_8, Branch.NONE, 0, 0, 0, 0, "shield"),
    
    // 序列7 - 腐化大师
    CORRUPTION_CONTROL("腐化操控", "corruption_control", SkillType.ACTIVE, Pathway.CORRUPTION, Sequence.SEQ_7, Branch.NONE, 2.0, 8.0, 15, 30, "control"),
    CORRUPTION_ERODE("腐化侵蚀", "corruption_erode", SkillType.PASSIVE, Pathway.CORRUPTION, Sequence.SEQ_7, Branch.NONE, 0, 0, 0, 0, "erode"),
    
    // 序列7 - 治愈者分支
    CORRUPTION_HEAL_GROUP("腐化群体治愈", "corruption_heal_group", SkillType.ACTIVE, Pathway.CORRUPTION, Sequence.SEQ_7, Branch.HEALER, 0, 10.0, 20, 40, "heal_group"),
    CORRUPTION_REGEN("腐化再生", "corruption_regen", SkillType.PASSIVE, Pathway.CORRUPTION, Sequence.SEQ_7, Branch.HEALER, 0, 0, 0, 0, "regen"),
    
    // 序列7 - 诅咒者分支
    CORRUPTION_CURSE("腐化诅咒", "corruption_curse", SkillType.ACTIVE, Pathway.CORRUPTION, Sequence.SEQ_7, Branch.CURSER, 2.0, 8.0, 18, 35, "curse"),
    CORRUPTION_WEAKNESS("腐化虚弱", "corruption_weakness", SkillType.PASSIVE, Pathway.CORRUPTION, Sequence.SEQ_7, Branch.CURSER, 0, 0, 0, 0, "weakness");
    
    private final String name;
    private final String id;
    private final SkillType type;
    private final Pathway pathway;
    private final Sequence sequence;
    private final Branch branch;
    private final double damageMult;
    private final double range;
    private final int cooldown;
    private final int manaCost;
    private final String effect;
    
    Skill(String name, String id, SkillType type, Pathway pathway, Sequence sequence, Branch branch,
          double damageMult, double range, int cooldown, int manaCost, String effect) {
        this.name = name;
        this.id = id;
        this.type = type;
        this.pathway = pathway;
        this.sequence = sequence;
        this.branch = branch;
        this.damageMult = damageMult;
        this.range = range;
        this.cooldown = cooldown;
        this.manaCost = manaCost;
        this.effect = effect;
    }
    
    public String getName() { return name; }
    public String getId() { return id; }
    public SkillType getType() { return type; }
    public Pathway getPathway() { return pathway; }
    public Sequence getSequence() { return sequence; }
    public Branch getBranch() { return branch; }
    public double getDamageMult() { return damageMult; }
    public double getRange() { return range; }
    public int getCooldown() { return cooldown; }
    public int getManaCost() { return manaCost; }
    public String getEffect() { return effect; }
    
    public boolean isActive() { return type == SkillType.ACTIVE; }
    public boolean isPassive() { return type == SkillType.PASSIVE; }
    
    /**
     * 根据ID获取技能
     */
    public static Skill fromId(String id) {
        if (id == null) return null;
        switch (id) {
            case "abyss_slash": return ABYSS_SLASH;
            case "abyss_sense": return ABYSS_SENSE;
            case "dark_shield": return DARK_SHIELD;
            case "abyss_resilience": return ABYSS_RESILIENCE;
            case "abyss_charge": return ABYSS_CHARGE;
            case "abyss_power": return ABYSS_POWER;
            case "abyss_fury": return ABYSS_FURY;
            case "abyss_rage": return ABYSS_RAGE;
            case "abyss_guard": return ABYSS_GUARD;
            case "abyss_wall": return ABYSS_WALL;
            case "abyss_summon": return ABYSS_SUMMON;
            case "abyss_pressure": return ABYSS_PRESSURE;
            case "abyss_judgment": return ABYSS_JUDGMENT;
            case "abyss_devour": return ABYSS_DEVOUR;
            case "abyss_annihilate": return ABYSS_ANNIHILATE;
            case "abyss_avatar": return ABYSS_AVATAR;
            case "abyss_realm": return ABYSS_REALM;
            case "abyss_rule": return ABYSS_RULE;
            case "abyss_will": return ABYSS_WILL;
            case "abyss_immortal": return ABYSS_IMMORTAL;
            case "abyss_god_power": return ABYSS_GOD_POWER;
            case "abyss_create": return ABYSS_CREATE;
            case "abyss_creation": return ABYSS_CREATION;
            case "abyss_domination": return ABYSS_DOMINATION;
            case "void_fireball": return VOID_FIREBALL;
            case "void_sense": return VOID_SENSE;
            case "void_ice": return VOID_ICE;
            case "void_shield": return VOID_SHIELD;
            case "void_control": return VOID_CONTROL;
            case "void_erode": return VOID_ERODE;
            case "void_burst": return VOID_BURST;
            case "void_destroy": return VOID_DESTROY;
            case "void_bind": return VOID_BIND;
            case "void_dominate": return VOID_DOMINATE;
            case "void_domain": return VOID_DOMAIN;
            case "void_meteor": return VOID_METEOR;
            case "void_collapse": return VOID_COLLAPSE;
            case "void_annihilate": return VOID_ANNIHILATE;
            case "void_avatar": return VOID_AVATAR;
            case "time_stop": return TIME_STOP;
            case "void_eternal": return VOID_ETERNAL;
            case "shadow_stealth": return SHADOW_STEALTH;
            case "shadow_sense": return SHADOW_SENSE;
            case "shadow_backstab": return SHADOW_BACKSTAB;
            case "shadow_track": return SHADOW_TRACK;
            case "shadow_storm": return SHADOW_STORM;
            case "shadow_mastery": return SHADOW_MASTERY;
            case "shadow_kill": return SHADOW_KILL;
            case "shadow_mark": return SHADOW_MARK;
            case "shadow_dance": return SHADOW_DANCE;
            case "shadow_wave": return SHADOW_WAVE;
            case "corruption_heal": return CORRUPTION_HEAL;
            case "corruption_sense": return CORRUPTION_SENSE;
            case "taboo_blessing": return TABOO_BLESSING;
            case "corruption_shield": return CORRUPTION_SHIELD;
            case "corruption_control": return CORRUPTION_CONTROL;
            case "corruption_erode": return CORRUPTION_ERODE;
            case "corruption_heal_group": return CORRUPTION_HEAL_GROUP;
            case "corruption_regen": return CORRUPTION_REGEN;
            case "corruption_curse": return CORRUPTION_CURSE;
            case "corruption_weakness": return CORRUPTION_WEAKNESS;
            default: return null;
        }
    }
    
    /**
     * 获取途径的所有技能
     */
    public static Skill[] getSkillsByPathway(Pathway pathway) {
        if (pathway == null) return new Skill[0];
        switch (pathway) {
            case ABYSS: return new Skill[]{
                ABYSS_SLASH, ABYSS_SENSE, DARK_SHIELD, ABYSS_RESILIENCE,
                ABYSS_CHARGE, ABYSS_POWER, ABYSS_FURY, ABYSS_RAGE,
                ABYSS_GUARD, ABYSS_WALL, ABYSS_SUMMON, ABYSS_PRESSURE,
                ABYSS_JUDGMENT, ABYSS_DEVOUR, ABYSS_ANNIHILATE, ABYSS_AVATAR,
                ABYSS_REALM, ABYSS_RULE, ABYSS_WILL, ABYSS_IMMORTAL,
                ABYSS_GOD_POWER, ABYSS_CREATE, ABYSS_CREATION, ABYSS_DOMINATION
            };
            case VOID: return new Skill[]{
                VOID_FIREBALL, VOID_SENSE, VOID_ICE, VOID_SHIELD,
                VOID_CONTROL, VOID_ERODE, VOID_BURST, VOID_DESTROY,
                VOID_BIND, VOID_DOMINATE, VOID_DOMAIN, VOID_METEOR,
                VOID_COLLAPSE, VOID_ANNIHILATE, VOID_AVATAR, TIME_STOP, VOID_ETERNAL
            };
            case SHADOW: return new Skill[]{
                SHADOW_STEALTH, SHADOW_SENSE, SHADOW_BACKSTAB, SHADOW_TRACK,
                SHADOW_STORM, SHADOW_MASTERY, SHADOW_KILL, SHADOW_MARK,
                SHADOW_DANCE, SHADOW_WAVE
            };
            case CORRUPTION: return new Skill[]{
                CORRUPTION_HEAL, CORRUPTION_SENSE, TABOO_BLESSING, CORRUPTION_SHIELD,
                CORRUPTION_CONTROL, CORRUPTION_ERODE, CORRUPTION_HEAL_GROUP, CORRUPTION_REGEN,
                CORRUPTION_CURSE, CORRUPTION_WEAKNESS
            };
            default: return new Skill[0];
        }
    }
    
    /**
     * 获取序列的所有技能
     */
    public static Skill[] getSkillsBySequence(Sequence sequence) {
        if (sequence == null) return new Skill[0];
        switch (sequence) {
            case SEQ_9: return new Skill[]{
                ABYSS_SLASH, ABYSS_SENSE, VOID_FIREBALL, VOID_SENSE,
                SHADOW_STEALTH, SHADOW_SENSE, CORRUPTION_HEAL, CORRUPTION_SENSE
            };
            case SEQ_8: return new Skill[]{
                DARK_SHIELD, ABYSS_RESILIENCE, VOID_ICE, VOID_SHIELD,
                SHADOW_BACKSTAB, SHADOW_TRACK, TABOO_BLESSING, CORRUPTION_SHIELD
            };
            case SEQ_7: return new Skill[]{
                ABYSS_CHARGE, ABYSS_POWER, ABYSS_FURY, ABYSS_RAGE,
                ABYSS_GUARD, ABYSS_WALL, VOID_CONTROL, VOID_ERODE,
                VOID_BURST, VOID_DESTROY, VOID_BIND, VOID_DOMINATE,
                SHADOW_STORM, SHADOW_MASTERY, SHADOW_KILL, SHADOW_MARK,
                SHADOW_DANCE, SHADOW_WAVE, CORRUPTION_CONTROL, CORRUPTION_ERODE,
                CORRUPTION_HEAL_GROUP, CORRUPTION_REGEN, CORRUPTION_CURSE, CORRUPTION_WEAKNESS
            };
            case SEQ_6: return new Skill[]{ABYSS_SUMMON, ABYSS_PRESSURE, VOID_DOMAIN};
            case SEQ_5: return new Skill[]{ABYSS_JUDGMENT, ABYSS_DEVOUR, VOID_METEOR, VOID_COLLAPSE};
            case SEQ_4: return new Skill[]{ABYSS_ANNIHILATE, ABYSS_AVATAR, VOID_ANNIHILATE, VOID_AVATAR};
            case SEQ_3: return new Skill[]{ABYSS_REALM, ABYSS_RULE, TIME_STOP, VOID_ETERNAL};
            case SEQ_2: return new Skill[]{ABYSS_WILL, ABYSS_IMMORTAL};
            case SEQ_1: return new Skill[]{ABYSS_GOD_POWER, ABYSS_CREATE};
            case SEQ_0: return new Skill[]{ABYSS_CREATION, ABYSS_DOMINATION};
            default: return new Skill[0];
        }
    }
}